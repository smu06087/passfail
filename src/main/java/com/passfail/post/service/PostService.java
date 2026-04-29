package com.passfail.post.service;

import com.passfail.entity.PostEntity;
import com.passfail.entity.PostLikeEntity;
import com.passfail.enums.PostCategory;
import com.passfail.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;
    private final RedisViewService redisViewService;
    private final RedisLikeService redisLikeService;

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
    @Transactional // ✅ 조회수 업데이트를 위해 readOnly 제거
    public PostDetailResponseDTO getPostDetail(Long postId, String currentUsername) {
        
        // 1. 게시글 조회 (Fetch Join으로 Member까지 한 번에 로드)
        PostEntity post = postRepository.findByIdWithMember(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        boolean isLiked = false;
        Long currentMemberId = null;

        // 2. 로그인 사용자 처리 (조회수 중복 방지 + 좋아요 여부 확인)
        if (currentUsername != null) {
            // username으로 memberId 조회
            var member = memberRepository.findByUsername(currentUsername)
                    .orElse(null);

            if (member != null) {
                currentMemberId = member.getMemberId();

                // [A] 조회수 중복 방지 (Redis 활용)
                if (redisViewService.isNewView(postId, currentMemberId)) {
                    postRepository.incrementViewCount(postId);
                    // JPA 1차 캐시 수동 동기화 (화면 표시용)
                    post.setViewCount(post.getViewCount() + 1);
                }

                // [B] 좋아요 여부 확인 (기존 RedisLikeService 활용)
                // 서비스 클래스 상단에 RedisLikeService 주입 확인 필요!
                isLiked = redisLikeService.isLiked(postId, currentMemberId);
            }
        }

        // 3. 댓글 목록 가져오기 (CommentService 활용)
        // 현재 접속자 ID를 넘겨야 댓글 작성자 본인 확인(isAuthor)이 가능합니다.
        List<CommentResponseDTO> comments = commentService.getComments(postId, currentMemberId);

        // 4. DTO 변환 및 반환
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