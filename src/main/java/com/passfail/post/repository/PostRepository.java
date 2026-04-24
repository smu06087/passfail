package com.passfail.post.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.passfail.entity.PostEntity;
import com.passfail.enums.PostCategory;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

    // 카테고리 필터 목록 조회
    Page<PostEntity> findByIsDeletedFalseAndCategoryOrderByIsPinnedDescCreatedAtDesc(
            PostCategory category, Pageable pageable);

    // 전체 목록 조회
    Page<PostEntity> findByIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);

    // ── 검색 (전체) ──────────────────────────────────────────────────
    // ✅ 수정: JPQL의 ORDER BY 제거 → Pageable 정렬과 충돌 방지
    // PostController의 @PageableDefault(sort="createdAt", direction=DESC)가 정렬 담당
    // isPinned 우선 정렬이 필요하면 PostService에서 PageRequest.of(page, size, sort) 직접 생성
    @Query("SELECT p FROM PostEntity p " +
           "WHERE p.isDeleted = false " +
           "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<PostEntity> searchByKeywordOnly(
            @Param("keyword") String keyword,
            Pageable pageable);

    // ── 검색 (카테고리 + 키워드) ──────────────────────────────────────
    // ✅ 수정: 동일하게 ORDER BY 제거
    @Query("SELECT p FROM PostEntity p " +
           "WHERE p.isDeleted = false " +
           "AND p.category = :category " +
           "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<PostEntity> searchByKeywordAndCategory(
            @Param("category") PostCategory category,
            @Param("keyword")  String keyword,
            Pageable pageable);

    // ── @Modifying 전체 수정 ─────────────────────────────────────────
    // ✅ clearAutomatically=true : 1차 캐시 자동 초기화 → DB와 영속성 컨텍스트 불일치 방지
    // ✅ @Transactional 명시    : Repository 레벨에서도 트랜잭션 보장 (서비스가 없을 때 단독 호출 안전)

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE PostEntity p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
    void incrementViewCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE PostEntity p SET p.likeCount = p.likeCount + 1 WHERE p.postId = :postId")
    void incrementLikeCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE PostEntity p SET p.likeCount = p.likeCount - 1 WHERE p.postId = :postId AND p.likeCount > 0")
    void decrementLikeCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE PostEntity p SET p.commentCount = p.commentCount + 1 WHERE p.postId = :postId")
    void incrementCommentCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE PostEntity p SET p.commentCount = p.commentCount - 1 WHERE p.postId = :postId AND p.commentCount > 0")
    void decrementCommentCount(@Param("postId") Long postId);
}