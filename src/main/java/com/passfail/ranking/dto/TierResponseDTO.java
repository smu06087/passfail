package com.passfail.ranking.dto;

import com.passfail.enums.Tier;
import com.passfail.enums.TierType;

import lombok.Builder;
import lombok.Getter;

/*
 * 🎯 티어 정보 뷰 전달 DTO
 * - DB 점수(Integer) → Tier.fromScore() → 뷰 표시 정보 변환
 * - 데일리/총 티어 공통 사용
 */
@Getter
@Builder
public class TierResponseDTO {
	
	private TierType tierType;         // DAILY or TOTAL

    private int     currentScore;      // 현재 점수
    private String  tierName;          // "골드"
    private String  tierIcon;          // "🥇"
    private String  tierColorCode;     // "#FFD700"
    private int     minScore;          // 현재 티어 최소 점수
    private int     maxScore;          // 현재 티어 최대 점수
    private int     scoreToNextTier;   // 다음 티어까지 필요 점수
    private int     progressPercent;   // 현재 티어 내 진행률 (0~100)
    private boolean isMaxTier;         // 최고 티어(RUBY) 여부
    private Integer rank;              // 현재 랭킹 (null 가능)
    
    //점수와 티어 타입 넘기면 DTO자동 생성
    public static TierResponseDTO fromScore(int score, TierType tierType, Integer rank) {
    	
    	Tier tier = Tier.fromScore(score);
    	return TierResponseDTO.builder()
    			.tierType(tierType)
    			.currentScore(score)
    			.tierName(tier.getDisplayName())
    			.tierIcon(tier.getIcon())
    			.tierColorCode(tier.getColorCode())
    			.minScore(tier.getMinScore())
    			.maxScore(tier.getMaxScore())
    			.scoreToNextTier(tier.scoreToNextTier(score))
    			.progressPercent(tier.progressPercent(score))
    			.isMaxTier(tier == Tier.RUBY)
    			.rank(rank)
    			.build();
    	
    }
	
}
