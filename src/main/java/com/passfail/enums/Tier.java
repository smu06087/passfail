package com.passfail.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Tier {
	
	BRONZE  (0,      299,   "#CD7F32"),  // 🥉
    SILVER  (300,    799,   "#C0C0C0"),  // 🥈
    GOLD    (800,    1999,  "#FFD700"),  // 🥇
    PLATINUM(2000,   4999,  "#00CED1"),  // 💎
    DIAMOND (5000,   9999,  "#B9F2FF"),  // 💠
    RUBY    (10000,  Integer.MAX_VALUE, "#FF0000"); // 🔴

    private final int minScore;
    private final int maxScore;
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
	
}
