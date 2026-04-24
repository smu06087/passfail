package com.passfail.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Tier {
	
    BRONZE  ("브론즈",  0,      299,   "🥉", "#CD7F32"),
    SILVER  ("실버",    300,    799,   "🥈", "#C0C0C0"),
    GOLD    ("골드",    800,    1999,  "🥇", "#FFD700"),
    PLATINUM("플래티넘", 2000,   4999,  "💎", "#00CED1"),
    DIAMOND ("다이아",  5000,   9999,  "💠", "#B9F2FF"),
    RUBY    ("루비",    10000,  Integer.MAX_VALUE, "🔴", "#FF0000");
	
	private final String displayName;
    private final int minScore;
    private final int maxScore;
    private final String icon;
    private final String colorCode;

    // 점수 → 티어 자동 계산
    public static Tier fromScore(int score) {
        for (Tier tier : values()) {
            if (score >= tier.minScore && score <= tier.maxScore) {
                return tier;
            }
        }
        return BRONZE;
    }
    
    // 다음 티어까지 남은 점수
    public int scoreToNextTier(int currentScore) {
    	
    	if(this == RUBY) {
    		return 0;
    	}
    	
    	Tier next = values()[this.ordinal() + 1];	//enums 안의 값()[몇 번째 인덱스] => enums리스트의 몇번째 인덱스 값 가져 와
    	
    	return next.minScore - currentScore;
    }
    
    // 현재 티어내 진행률
    public int progressPercent(int currentScore) {
    	
    	if(this == RUBY) {
    		return 100;
    	}
    	
    	int range = this.maxScore - this.minScore + 1;
    	int progress = currentScore - this.minScore;
    	
    	return (int) ((progress / (double)range) * 100);
    	
    }
	
}
