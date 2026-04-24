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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository     postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository  commentRepository;
    private final RedisLikeService   redisLikeService;
    private final RedisViewService   redisViewService;

    // ── 게시글 목록 조회 ──────────────────────────────────────────
 // ✅ null-safe SpEL: DB 데이터 없을 때 pageable.pageNumber NPE 방지
    @Cacheable(
        value = "postList",
        key = "(#category != null ? #category.name() : 'ALL') + '_' + (#pageable != null ? #pageable.pageNumber : 0)"
    )
    @Transactional(readOnly = true)
    public Page<PostListResponseDTO> getPostList(PostCategory category, Pageable pageable) {
        Page<PostEntity> posts = (category == null)
                ? postRepository.findByIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(pageable)
                : postRepository.findByIsDeletedFalseAndCategoryOrderByIsPinnedDescCreatedAtDesc(category, pageable);

        return posts.map(PostListResponseDTO::from);
    }

    // ── 게시글 검색 ────────────────────────────────────────────────
    // ✅ 수정: isPinned DESC + createdAt DESC 정렬을 PageRequest로 직접 생성
    // → JPQL ORDER BY를 제거했으므로 여기서 정렬을 명시적으로 제어
    @Transactional(readOnly = true)
    public Page<PostListResponseDTO> searchPosts(
            PostCategory category, String keyword, Pageable pageable) {

        // isPinned 우선 → createdAt 내림차순 정렬 보장
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

    // ── 게시글 상세 조회 ──────────────────────────────────────────
    @Transactional
    public PostDetailResponseDTO getPostDetail(Long postId, Long currentMemberId) {
        PostEntity post = getActivePost(postId);

        // 중복 조회수 방지: 비로그인 → 항상 증가 / 로그인 → Redis로 24시간 중복 체크
        if (currentMemberId == null || redisViewService.isNewView(postId, currentMemberId)) {
            postRepository.incrementViewCount(postId);
        }

        // ✅ 수정: Redis miss 시 DB fallback → 좋아요 중복/취소 불가 버그 방지
        boolean isLiked = false;
        if (currentMemberId != null) {
            isLiked = isLikedWithFallback(postId, currentMemberId);
        }

        List<CommentResponseDTO> comments = commentRepository
                .findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId)
                .stream()
                .map(CommentResponseDTO::from)
                .toList();

        return PostDetailResponseDTO.from(post, isLiked, comments);
    }

    // ── 게시글 작성 ────────────────────────────────────────────────
    // ✅ @CacheEvict: 글 작성 시 postList 캐시 전체 무효화
    @CacheEvict(value = "postList", allEntries = true)
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

    // ── 게시글 수정 ────────────────────────────────────────────────
    // ✅ @CacheEvict: 글 수정 시 postList 캐시 전체 무효화
    @CacheEvict(value = "postList", allEntries = true)
    @Transactional
    public void updatePost(Long postId, PostUpdateRequestDTO dto, Long currentMemberId) {
        PostEntity post = getActivePost(postId);
        validateAuthor(post, currentMemberId);

        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        // dirty checking으로 자동 저장
    }

    // ── 게시글 삭제 ────────────────────────────────────────────────
    // ✅ @CacheEvict: 글 삭제 시 postList 캐시 전체 무효화
    @CacheEvict(value = "postList", allEntries = true)
    @Transactional
    public void deletePost(Long postId, Long currentMemberId) {
        PostEntity post = getActivePost(postId);
        validateAuthor(post, currentMemberId);

        post.setIsDeleted(true);
    }

    // ── 좋아요 토글 ────────────────────────────────────────────────
    // ✅ 수정: Redis miss 시 DB fallback 포함
    @Transactional
    public boolean toggleLike(Long postId, Long memberId) {
        getActivePost(postId); // 존재 여부 검증

        // Redis 확인 → Redis에 없으면 DB에서 확인 (fallback)
        boolean alreadyLiked = isLikedWithFallback(postId, memberId);

        if (alreadyLiked) {
            // 좋아요 취소
            redisLikeService.removeLike(postId, memberId);
            postLikeRepository.findByPostIdAndMemberId(postId, memberId)
                    .ifPresent(postLikeRepository::delete);
            postRepository.decrementLikeCount(postId);
            return false;
        } else {
            // 좋아요 추가
            redisLikeService.addLike(postId, memberId);
            postLikeRepository.save(PostLikeEntity.builder()
                    .postId(postId)
                    .memberId(memberId)
                    .build());
            postRepository.incrementLikeCount(postId);
            return true;
        }
    }

    // ── 고정글 설정 (관리자 전용 - 추후 @PreAuthorize 추가 예정) ────
    @CacheEvict(value = "postList", allEntries = true)
    @Transactional
    public void togglePin(Long postId) {
        PostEntity post = getActivePost(postId);
        post.setIsPinned(!post.getIsPinned());
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────

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

    /**
     * ✅ Redis miss → DB fallback 로직
     * Redis에 데이터가 없을 때(재시작/TTL만료) DB에서 확인 후 Redis를 동기화한다.
     * 이렇게 하면 좋아요 중복 저장 / 취소 불가 버그를 방지할 수 있다.
     */
    private boolean isLikedWithFallback(Long postId, Long memberId) {
        boolean redisResult = redisLikeService.isLiked(postId, memberId);

        if (!redisResult) {
            // Redis에 없으면 DB 확인
            boolean dbResult = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
            if (dbResult) {
                // DB에 좋아요 있음 → Redis 동기화 (warm-up)
                redisLikeService.addLike(postId, memberId);
            }
            return dbResult;
        }

        return true;
    }
}