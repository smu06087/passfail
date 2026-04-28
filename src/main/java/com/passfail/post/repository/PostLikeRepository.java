package com.passfail.post.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.passfail.entity.PostLikeEntity;

public interface PostLikeRepository extends JpaRepository<PostLikeEntity, Long>{
	
	// 특정 게시글에 특정 멤버가 좋아요를 눌렀는지 확인(null일 수 있기 때문에 Optional을 사용
	Optional<PostLikeEntity> findByPostIdAndMemberId(Long postId, Long memberId);
	
	// 특정 게시글에 특정 멤버의 좋아요 존재 여부
	boolean existsByPostIdAndMemberId(Long postId, Long memberId);
	
	// ✅ 새로 추가: 특정 게시글에 특정 멤버의 좋아요 삭제
	@Modifying
	@Query("DELETE FROM PostLikeEntity p WHERE p.post.postId = :postId AND p.memberId = :memberId")
	void deleteByPostIdAndMemberId(@Param("postId") Long postId, @Param("memberId") Long memberId);
	
}
