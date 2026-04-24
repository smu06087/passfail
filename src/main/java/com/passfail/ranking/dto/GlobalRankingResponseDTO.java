package com.passfail.ranking.dto;

import com.passfail.entity.GlobalRankingEntity;
import com.passfail.enums.Tier;

import lombok.Builder;
import lombok.Getter;

/*
 * 🌐 전체 등수 뷰 전달 DTO
 * - DB combinedScore(Integer) → Tier.fromScore() → 뷰 표시 정보 변환
 * - 순위 변동 방향/폭 포함
 */
@Getter
@Builder
public class GlobalRankingResponseDTO {
	
	// ── 회원 기본 정보 ────────────────────────────────────────
    private Long    memberId;
    private String  username;

    // ── 순위 정보 ─────────────────────────────────────────────
    private Integer globalRank;         // 전체 순위
    private Integer previousRank;       // 직전 순위
    private Integer rankChange;         // 순위 변동폭 (양수: 상승 / 음수: 하락)
    private String  rankDirection;      // "UP" / "DOWN" / "SAME"

    // ── 점수 정보 ─────────────────────────────────────────────
    private Integer combinedScore;      // 통합 점수 (문제 + 배틀)
    private Integer totalTierScore;     // 문제 풀이 점수
    private Integer dailyBonusScore;    // 배틀 보너스 점수

    // ── 티어 정보 ─────────────────────────────────────────────
    private String  tierName;           // "골드"
    private String  tierIcon;           // "🥇"
    private String  tierColorCode;      // "#FFD700"
    private int     progressPercent;    // 현재 티어 내 진행률 (0~100)
    private int     scoreToNextTier;    // 다음 티어까지 남은 점수
    private boolean isMaxTier;          // 최고 티어(RUBY) 여부

    // ── 통계 정보 ─────────────────────────────────────────────
    private Integer totalSolvedCount;   // 총 정답 문제 수
    private Integer totalBattleWin;     // 총 배틀 승리
    private Integer totalBattleLose;    // 총 배틀 패배
    private Double  battleWinRate;      // 배틀 승률 (%)
    
    //Entity --> DTO변환
    public static GlobalRankingResponseDTO fromEntity(GlobalRankingEntity entity, String username) {
    	
    	Tier tier = Tier.fromScore(entity.getCombinedScore());
    	
    	return GlobalRankingResponseDTO.builder()
                .memberId        (entity.getMemberId())
                .username        (username)
                .globalRank      (entity.getGlobalRank())
                .previousRank    (entity.getPreviousRank())
                .rankChange      (entity.getRankChange())
                .rankDirection   (entity.rankDirection())
                .combinedScore   (entity.getCombinedScore())
                .totalTierScore  (entity.getTotalTierScore())
                .dailyBonusScore (entity.getDailyBonusScore())
                .tierName        (tier.getDisplayName())
                .tierIcon        (tier.getIcon())
                .tierColorCode   (tier.getColorCode())
                .progressPercent (tier.progressPercent(entity.getCombinedScore()))
                .scoreToNextTier (tier.scoreToNextTier(entity.getCombinedScore()))
                .isMaxTier       (tier == Tier.RUBY)
                .totalSolvedCount(entity.getTotalSolvedCount())
                .totalBattleWin  (entity.getTotalBattleWin())
                .totalBattleLose (entity.getTotalBattleLose())
                .battleWinRate   (entity.getBattleWinRate())
                .build();
    	
    }
    
	
}
