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
     * [최적화] 점수 높은 순으로 상위 100명 조회
     * - isActive가 true인 회원만 포함
     * - 로컬 계정(password 존재)이거나 소셜 연동 계정(social_accounts 존재)인 경우만 포함
     */
    @Query("SELECT t FROM TotalTierEntity t " +
           "JOIN t.member m " +
           "WHERE m.isActive = true " +
           "AND (m.password IS NOT NULL OR size(m.social_accounts) > 0) " +
           "ORDER BY t.totalScore DESC")
    List<TotalTierEntity> findTop100WithFilter();

    /**
     * [기존 방식 호환용] 상위 100명 조회 (필터 적용)
     */
    default List<TotalTierEntity> findTop100ByOrderByTotalScoreDesc() {
        return findTop100WithFilter();
    }

    /**
     * [최적화] JPQL로 Member 정보를 JOIN FETCH하여 한 번에 조회
     * @param pageable 페이지 정보 (limit, offset)
     * @return MemberEntity 정보를 포함한 TotalTierEntity 리스트
     */
    @Query("SELECT t FROM TotalTierEntity t " +
           "JOIN FETCH t.member m " +
           "WHERE m.isActive = true " +
           "AND (m.password IS NOT NULL OR size(m.social_accounts) > 0) " +
           "ORDER BY t.currentRank ASC")
    List<TotalTierEntity> findRankingBoardWithMember(Pageable pageable);

    /**
     * [최적화] DTO로 직접 쿼리하여 필요한 필드만 조회
     */
    @Query("SELECT new com.passfail.ranking.dto.RankingResponseDTO(" +
           "m.memberId, m.username, t.totalScore, t.currentRank, t.tier) " +
           "FROM TotalTierEntity t " +
           "JOIN t.member m " +
           "WHERE m.isActive = true " +
           "AND (m.password IS NOT NULL OR size(m.social_accounts) > 0) " +
           "ORDER BY t.currentRank ASC")
    List<RankingResponseDTO> findRankingBoardDTO(Pageable pageable);

    /**
     * 배치 업데이트용: 점수순으로 전체 유저 조회 (필터 적용)
     */
    @Query("SELECT t FROM TotalTierEntity t " +
           "JOIN t.member m " +
           "WHERE m.isActive = true " +
           "AND (m.password IS NOT NULL OR size(m.social_accounts) > 0) " +
           "ORDER BY t.totalScore DESC")
    List<TotalTierEntity> findAllActiveWithFilter();

    /**
     * [기존 방식 호환용] 전체 조회 (필터 적용)
     */
    default List<TotalTierEntity> findAllByOrderByTotalScoreDesc() {
        return findAllActiveWithFilter();
    }

    /**
     * 특정 회원의 랭킹 정보 조회
     */
    Optional<TotalTierEntity> findByMember_MemberId(Long memberId);
}
