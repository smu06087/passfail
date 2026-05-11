package com.passfail.ranking.repository;

import com.passfail.entity.TotalTierEntity;
import com.passfail.ranking.dto.RankingResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TotalTierRepository extends JpaRepository<TotalTierEntity, Long> {

    /**
     * [기존 방식] 점수 높은 순으로 상위 100명 조회
     * 문제: N+1 발생 (member 정보 조회마다 추가 쿼리)
     */
    List<TotalTierEntity> findTop100ByOrderByTotalScoreDesc();

    /**
     * [최적화] JPQL로 Member 정보를 JOIN FETCH하여 한 번에 조회
     * @param pageable 페이지 정보 (limit, offset)
     * @return MemberEntity 정보를 포함한 TotalTierEntity 리스트
     */
    @Query("SELECT t FROM TotalTierEntity t " +
           "JOIN FETCH t.member m " +
           "ORDER BY t.currentRank ASC")
    List<TotalTierEntity> findRankingBoardWithMember(Pageable pageable);

    /**
     * [최적화] DTO로 직접 쿼리하여 필요한 필드만 조회 (가장 효율적)
     * - username을 MemberEntity에서 직접 긁어옴
     * - N+1 문제 완전 해결
     * 
     * @param pageable 페이지 정보
     * @return RankingResponseDTO 리스트
     */
    @Query("SELECT new com.passfail.ranking.dto.RankingResponseDTO(" +
           "m.memberId, m.username, t.totalScore, t.currentRank, t.tier) " +
           "FROM TotalTierEntity t " +
           "JOIN t.member m " +
           "ORDER BY t.currentRank ASC")
    List<RankingResponseDTO> findRankingBoardDTO(Pageable pageable);

    /**
     * 배치 업데이트용: 점수순으로 전체 유저 조회
     * 순위를 재계산하기 위해 내림차순 정렬
     */
    List<TotalTierEntity> findAllByOrderByTotalScoreDesc();

    /**
     * 특정 회원의 랭킹 정보 조회
     * @param memberId 조회할 회원 ID
     * @return TotalTierEntity Optional
     */
    Optional<TotalTierEntity> findByMember_MemberId(Long memberId);
}
