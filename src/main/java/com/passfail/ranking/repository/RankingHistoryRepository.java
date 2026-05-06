package com.passfail.ranking.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.passfail.entity.RankingHistoryEntity;

public interface RankingHistoryRepository extends JpaRepository<RankingHistoryEntity, Long>{
	
	@Query("SELECT r FROM RankingHistoryEntity r WHERE r.member.memberId = :memberId ORDER BY r.recordedDate ASC")
    List<RankingHistoryEntity> findByMemberIdOrderByRecordedDateAsc(@Param("memberId") Long memberId);
    
    // 특정 날짜의 전체 기록을 조회할 때 사용
    List<RankingHistoryEntity> findByRecordedDate(LocalDate date);
	
}
