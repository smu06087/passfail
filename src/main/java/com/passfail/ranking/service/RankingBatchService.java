package com.passfail.ranking.service;

import com.passfail.entity.RankingHistoryEntity;
import com.passfail.entity.TotalTierEntity;
import com.passfail.enums.Tier;
import com.passfail.ranking.repository.RankingHistoryRepository;
import com.passfail.ranking.repository.TotalTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 🌙 일일 배치 서비스
 * 
 * ─────────────────────────────────────────────────────────────
 * 실행 시간: 매일 자정 (00:00:00)
 * 
 * 작업 내용:
 *   1. 모든 사용자를 점수순으로 재정렬
 *   2. 순위(1위, 2위, ...) 자동 부여
 *   3. 티어 재확인 (실수 방지)
 *   4. RankingHistoryEntity에 스냅샷 저장 (추이 추적)
 * 
 * ⚠️ 주의사항:
 *   - 배치 실행 중 순위가 변경되므로 정확하지 않을 수 있음
 *   - 배치는 원자성 보장 (모두 성공 또는 모두 실패)
 * ─────────────────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class RankingBatchService {

    private final TotalTierRepository totalTierRepository;
    private final RankingHistoryRepository rankingHistoryRepository;

    /**
     * 🌙 매일 자정에 자동으로 실행되는 배치 작업
     * 
     * Cron Expression: "0 0 0 * * *"
     *   - 초:   0
     *   - 분:   0 (정각)
     *   - 시:   0 (자정)
     *   - 일:   * (매일)
     *   - 월:   * (매월)
     *   - 요일: * (매요일)
     */
    @Scheduled(cron = "0 0 0 * * *")  // 매일 자정 00:00
    @Transactional
    public void updateAllRankingsAtMidnight() {
        
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║ 🌙 [배치] 일일 랭킹 갱신 시작                                ║");
        System.out.println("║    실행 시간: " + startTime + "║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");

        // 1️⃣ 모든 사용자를 점수 높은 순으로 정렬
        List<TotalTierEntity> allUsers = totalTierRepository.findAllByOrderByTotalScoreDesc();
        System.out.println("📊 처리할 사용자 수: " + allUsers.size() + "명");

        // 2️⃣ 순위 재계산 및 이력 저장
        for (int i = 0; i < allUsers.size(); i++) {
            TotalTierEntity tierEntity = allUsers.get(i);
            int newRank = i + 1;  // 0부터 시작하므로 +1

            // ⭐ 순위 업데이트
            Tier tierAtScore = Tier.fromScore(tierEntity.getTotalScore());
            tierEntity.updateRankAndTier(newRank, tierAtScore);

            // ⭐ 이력 스냅샷 저장 (날짜별 추이 추적)
            RankingHistoryEntity history = RankingHistoryEntity.snapshotFrom(tierEntity, newRank);
            rankingHistoryRepository.save(history);

            // 10명마다 진행상황 출력
            if ((i + 1) % 10 == 0) {
                System.out.println("   ✓ " + (i + 1) + "명 처리 완료");
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        long duration = java.time.temporal.ChronoUnit.MILLIS.between(startTime, endTime);
        
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║ ✅ [배치] 일일 랭킹 갱신 완료                                ║");
        System.out.println("║    소요 시간: " + duration + "ms                              ║");
        System.out.println("║    처리 건수: " + allUsers.size() + "명                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }

    /**
     * 🔧 수동 배치 실행 (관리자용)
     * 
     * API: POST /ranking/update
     * 권한: ROLE_ADMIN만
     * 
     * 자정이 아닌데도 관리자가 수동으로 랭킹을 강제 갱신하고 싶을 때 사용
     */
    public void forceUpdate() {
        System.out.println("🔧 [수동] 관리자가 랭킹 갱신을 요청했습니다.");
        updateAllRankingsAtMidnight();  // 동일한 로직으로 실행
    }
}