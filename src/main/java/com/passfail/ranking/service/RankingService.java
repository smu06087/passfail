package com.passfail.ranking.service;

import com.passfail.entity.RankingHistoryEntity;
import com.passfail.entity.TotalTierEntity;
import com.passfail.enums.Tier;
import com.passfail.ranking.dto.RankingResponseDTO;
import com.passfail.ranking.repository.RankingHistoryRepository;
import com.passfail.ranking.repository.TotalTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final TotalTierRepository totalTierRepository;
    private final RankingHistoryRepository rankingHistoryRepository;

    /**
     * 🏆 상위 100명 랭킹 조회
     * ⭐ 수정: 실시간으로 순위를 계산해서 반환
     */
    @Transactional
    public List<RankingResponseDTO> getTopRankings() {
        System.out.println("📊 [조회] 상위 100명 랭킹 조회");
        
        // 1️⃣ 점수 높은 순으로 상위 100명 조회
        List<TotalTierEntity> top100 = totalTierRepository.findTop100ByOrderByTotalScoreDesc();
        
        if (top100.isEmpty()) {
            System.out.println("⚠️ 경고: TotalTierEntity 테이블이 비어있습니다!");
            return List.of();
        }

        // 2️⃣ 실시간 순위 계산 및 업데이트
        for (int i = 0; i < top100.size(); i++) {
            TotalTierEntity tier = top100.get(i);
            int realTimeRank = i + 1;  // 🎯 순위 = 인덱스 + 1
            
            // 3️⃣ 순위가 변경되었으면 업데이트
            if (!tier.getCurrentRank().equals(realTimeRank)) {
                tier.updateRankAndTier(realTimeRank, Tier.fromScore(tier.getTotalScore()));
                System.out.println("   ✓ " + tier.getMember().getUsername() + " → #" + realTimeRank);
            }
        }
        
        // 4️⃣ DTO 변환
        return top100.stream()
                .map(this::convertToDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 👤 개별 유저 랭킹 정보 조회
     */
    @Transactional(readOnly = true)
    public RankingResponseDTO getMyRanking(Long memberId) {
        TotalTierEntity myTier = totalTierRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new RuntimeException(
                        "회원 ID " + memberId + "의 랭킹 정보를 찾을 수 없습니다."));
        
        return convertToDTO(myTier);
    }

    /**
     * 🌙 일일 배치 (자정 자동 실행)
     * 또는 관리자가 수동으로 /ranking/update 호출
     */
    @Transactional
    public void refreshDailyRanking() {
        System.out.println("🌙 [배치] 일일 랭킹 갱신 시작");
        
        // 1️⃣ 모든 사용자를 점수 높은 순으로 정렬
        List<TotalTierEntity> allTiers = totalTierRepository.findAllByOrderByTotalScoreDesc();
        System.out.println("📊 처리 대상: " + allTiers.size() + "명");
        
        // 2️⃣ 순위와 티어 업데이트
        for (int i = 0; i < allTiers.size(); i++) {
            TotalTierEntity tierEntity = allTiers.get(i);
            int rank = i + 1;

            // ⭐ 순위 및 티어 업데이트 + lastUpdatedAt 자동 갱신
            tierEntity.updateRankAndTier(rank, Tier.fromScore(tierEntity.getTotalScore()));
            
            // 3️⃣ 이력 저장 (날짜별 추이 추적)
            RankingHistoryEntity history = RankingHistoryEntity.snapshotFrom(tierEntity, rank);
            rankingHistoryRepository.save(history);
        }
        
        System.out.println("✅ [배치] 일일 랭킹 갱신 완료");
    }

    /**
     * 🔧 내부 메서드: Entity → DTO 변환
     */
    private RankingResponseDTO convertToDTO(TotalTierEntity entity) {
        if (entity.getMember() == null) {
            System.out.println("❌ 오류: TOTAL_TIER_ID=" + entity.getTotalTierId() + 
                             "에 연결된 MemberEntity가 없습니다!");
            return null;
        }
        
        return RankingResponseDTO.builder()
                .memberId(entity.getMember().getMemberId())
                .username(entity.getMember().getUsername())
                .totalScore(entity.getTotalScore())
                .currentRank(entity.getCurrentRank())  // ⭐ 이제 정확함
                .tier(entity.getTier())                // ⭐ 이제 정확함
                .build();
    }
}