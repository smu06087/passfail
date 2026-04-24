package com.passfail.post.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.passfail.entity.CommentEntity;

public interface CommentRepository extends JpaRepository<CommentEntity, Long>{
	
	// 특정 게시글의 삭제되지 않은 댓글 목록 (작성일 오름차순)
	List<CommentEntity> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);
	
	// 특정 게시글의 댓글 수 (삭제 포함 — 필요 시 사용)
	long countByPostIdAndIsDeletedFalse(Long postId);
	
}
