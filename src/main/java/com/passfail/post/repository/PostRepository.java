package com.passfail.post.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.passfail.entity.PostEntity;
import com.passfail.enums.PostCategory;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
	
	// ✅ 이 메서드 1개만 추가
	// member를 JOIN FETCH로 함께 로드 → LazyInitializationException 방지
	 @Query("SELECT p FROM PostEntity p LEFT JOIN FETCH p.member WHERE p.postId = :postId")
	    Optional<PostEntity> findByIdWithMember(@Param("postId") Long postId);

    // ✅ 목록 조회 — member JOIN FETCH
    @EntityGraph(attributePaths = {"member"})
    Page<PostEntity> findByIsDeletedFalseAndCategoryOrderByIsPinnedDescCreatedAtDesc(
            PostCategory category, Pageable pageable);

    // ✅ 목록 조회 (전체) — member JOIN FETCH
    @EntityGraph(attributePaths = {"member"})
    Page<PostEntity> findByIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);

    // 검색 (키워드만)
    @Query("SELECT p FROM PostEntity p " +
           "WHERE p.isDeleted = false " +
           "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<PostEntity> searchByKeywordOnly(
            @Param("keyword") String keyword,
            Pageable pageable);

    // 검색 (카테고리 + 키워드)
    @Query("SELECT p FROM PostEntity p " +
           "WHERE p.isDeleted = false " +
           "AND p.category = :category " +
           "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<PostEntity> searchByKeywordAndCategory(
            @Param("category") PostCategory category,
            @Param("keyword")  String keyword,
            Pageable pageable);

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