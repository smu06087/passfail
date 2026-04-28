package com.passfail.post.service;

import com.passfail.entity.PostEntity;
import com.passfail.entity.PostLikeEntity;
import com.passfail.enums.PostCategory;
import com.passfail.post.dto.CommentResponseDTO;
import com.passfail.post.dto.PostCreateRequestDTO;
import com.passfail.post.dto.PostDetailResponseDTO;
import com.passfail.post.dto.PostListResponseDTO;
import com.passfail.post.dto.PostUpdateRequestDTO;
import com.passfail.post.exception.PostNotFoundException;
import com.passfail.post.exception.UnauthorizedPostAccessException;
import com.passfail.post.repository.CommentRepository;
import com.passfail.post.repository.PostLikeRepository;
import com.passfail.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CommentService commentService;

    private static final long VIEW_CACHE_TTL = 24;
    private static final TimeUnit VIEW_CACHE_UNIT = TimeUnit.HOURS;

    // ── 게시글 목록 ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<PostListResponseDTO> getPostList(PostCategory category, Pageable pageable) {
        Page<PostEntity> posts = (category == null)
                ? postRepository.findByIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(pageable)
                : postRepository.findByIsDeletedFalseAndCategoryOrderByIsPinnedDescCreatedAtDesc(category, pageable);
        return posts.map(PostListResponseDTO::from);
    }

    // ── 게시글 검색 ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<PostListResponseDTO> searchPosts(PostCategory category, String keyword, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("isPinned"), Sort.Order.desc("createdAt"))
        );

        Page<PostEntity> posts = (category == null)
                ? postRepository.searchByKeywordOnly(keyword, sortedPageable)
                : postRepository.searchByKeywordAndCategory(category, keyword, sortedPageable);

        return posts.map(PostListResponseDTO::from);
    }

 // ── 게시글 상세 조회 ──────────────────────────────────────────────
    @Transactional(readOnly = true) // ✅ 읽기 전용 트랜잭션 시작
    public PostDetailResponseDTO getPostDetail(Long postId, String currentUsername) {
        
        // ✅ 수정: findById -> findByIdWithMember 사용
        PostEntity post = postRepository.findByIdWithMember(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 댓글 목록 가져오기
        List<CommentResponseDTO> comments = commentService.getComments(postId);

        // 좋아요 여부 (기존 로직 유지)
        boolean isLiked = false; 

        // DTO 변환 (이제 post.getMember()가 이미 로딩되어 있어 에러가 나지 않음)
        return PostDetailResponseDTO.from(post, currentUsername, isLiked, comments);
    }

    // ── 게시글 작성 ──────────────────────────────────────────────
    @Transactional
    public Long createPost(PostCreateRequestDTO dto, Long memberId) {
        PostEntity post = PostEntity.builder()
                .memberId(memberId)
                .category(dto.getCategory())
                .title(dto.getTitle())
                .content(dto.getContent())
                .build();
        return postRepository.save(post).getPostId();
    }

    // ── 게시글 수정 ──────────────────────────────────────────────
    @Transactional
    public void updatePost(Long postId, PostUpdateRequestDTO dto, Long currentMemberId) {
        PostEntity post = getActivePost(postId);
        validateAuthor(post, currentMemberId);
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
    }

    // ── 게시글 삭제 ──────────────────────────────────────────────
    @Transactional
    public void deletePost(Long postId, Long currentMemberId) {
        PostEntity post = getActivePost(postId);
        validateAuthor(post, currentMemberId);
        post.setIsDeleted(true);
    }

    // ── 좋아요 토글 ──────────────────────────────────────────────
    // ✅ 완전 수정: postId 필드에 직접 값 설정
    @Transactional
    public boolean toggleLike(Long postId, Long memberId) {
        // 현재 좋아요 상태 확인
        boolean alreadyLiked = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);

        if (alreadyLiked) {
            // ✅ 좋아요 취소
            postLikeRepository.deleteByPostIdAndMemberId(postId, memberId);
            postRepository.decrementLikeCount(postId);
            return false;  // 취소됨
        } else {
            // ✅ 좋아요 추가: postId 필드에 직접 값 설정!
            PostLikeEntity like = PostLikeEntity.builder()
                    .postId(postId)  // ← post 객체 대신 postId 직접 설정
                    .memberId(memberId)
                    .build();
            postLikeRepository.save(like);
            postRepository.incrementLikeCount(postId);
            return true;  // 추가됨
        }
    }

    // ── 핀 토글 ──────────────────────────────────────────────────
    @Transactional
    public void togglePin(Long postId) {
        PostEntity post = getActivePost(postId);
        post.setIsPinned(!post.getIsPinned());
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────
    private PostEntity getActivePost(Long postId) {
        return postRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new PostNotFoundException(postId));
    }

    private void validateAuthor(PostEntity post, Long currentMemberId) {
        if (!post.getMemberId().equals(currentMemberId)) {
            throw new UnauthorizedPostAccessException();
        }
    }

    private boolean isNewView(Long postId, Long memberId) {
        String key = "view:" + postId + ":" + memberId;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(exists)) {
            redisTemplate.opsForValue().set(key, "1", VIEW_CACHE_TTL, VIEW_CACHE_UNIT);
            return true;
        }
        return false;
    }

    // 불필요한 Redis 메서드들은 제거 (DB 사용하므로)
}