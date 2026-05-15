package com.passfail.post.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.passfail.entity.CommentEntity;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    // ✅ @EntityGraph 추가: member 즉시 로딩 → LazyInitializationException 방지
    @EntityGraph(attributePaths = {"member"})
    List<CommentEntity> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);

    long countByPostIdAndIsDeletedFalse(Long postId);

    Page<CommentEntity> findByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(Long memberId, Pageable pageable);
}