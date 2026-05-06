package com.passfail.ranking.service;

import com.passfail.entity.RankingHistoryEntity;
import com.passfail.entity.TotalTierEntity;
import com.passfail.enums.Tier;
import com.passfail.ranking.dto.RankingResponseDTO;
import com.passfail.ranking.repository.RankingHistoryRepository;
import com.passfail.ranking.repository.TotalTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
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
     */
    @Transactional(readOnly = true)
    public List<RankingResponseDTO> getTopRankings() {
        System.out.println("📊 [조회] 상위 100명 랭킹 조회");
        
        List<TotalTierEntity> top100 = totalTierRepository.findTop100ByOrderByTotalScoreDesc();
        
        if (top100.isEmpty()) {
            System.out.println("⚠️ 경고: TotalTierEntity 테이블이 비어있습니다!");
            return List.of();
        }
        
        return top100.stream()
                .map(this::convertToDTO)
                .filter(Objects::nonNull) // Member가 없는 경우 제외
                .collect(Collectors.toList());
    }

    /**
     * 👤 내 랭킹 정보 조회
     */
    @Transactional(readOnly = true)
    public RankingResponseDTO getMyRanking(Long memberId) {
        // 🌟 수정: PK가 아니므로 findById 대신 findByMember_MemberId 사용
        TotalTierEntity myTier = totalTierRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new RuntimeException(
                        "회원 ID " + memberId + "의 랭킹 정보를 찾을 수 없습니다."));
        
        return convertToDTO(myTier);
    }

    /**
     * 🌙 매일 자정 자동 순위 갱신 배치
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void refreshDailyRanking() {
        System.out.println("🌙 [배치] 일일 랭킹 갱신 시작");
        
        List<TotalTierEntity> allTiers = totalTierRepository.findAllByOrderByTotalScoreDesc();
        System.out.println("📊 처리 대상: " + allTiers.size() + "명");
        
        for (int i = 0; i < allTiers.size(); i++) {
            TotalTierEntity tierEntity = allTiers.get(i);
            int rank = i + 1;

            // 순위 및 티어 업데이트
            tierEntity.updateRankAndTier(rank, Tier.fromScore(tierEntity.getTotalScore()));
            
            // 🌟 수정: 이력 저장 시 Entity에서 필요한 정보 추출
            RankingHistoryEntity history = RankingHistoryEntity.snapshotFrom(tierEntity, rank);
            rankingHistoryRepository.save(history);
        }
        
        System.out.println("✅ [배치] 일일 랭킹 갱신 완료");
    }

    /**
     * 🛠 Entity -> DTO 변환 로직 (오류 해결 핵심)
     */
    private RankingResponseDTO convertToDTO(TotalTierEntity entity) {
        // 🌟 수정: entity.getMemberId() 대신 연관 객체에서 ID 추출
        if (entity.getMember() == null) {
            System.out.println("❌ 오류: TOTAL_TIER_ID=" + entity.getTotalTierId() + 
                             "에 연결된 MemberEntity가 없습니다!");
            return null;
        }
        
        return RankingResponseDTO.builder()
                .memberId(entity.getMember().getMemberId()) // 👈 수정됨
                .username(entity.getMember().getUsername())
                .totalScore(entity.getTotalScore())
                .currentRank(entity.getCurrentRank())
                .tier(entity.getTier())
                .build();
    }
}
