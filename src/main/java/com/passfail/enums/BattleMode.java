package com.passfail.enums;

import lombok.Getter;

@Getter
public enum BattleMode {
    QUICK("빨리 풀기", true, false),      // 개인종료, 점수높은순(1/0) + 시간빠른순
    MANY("많이 풀기", false, true),       // 동시종료, 점수높은순
    SURVIVAL("서바이벌", false, true),    // 동시종료(마지막 생존)
    GOLF("코드 골프", true, false),      // 개인종료, 점수낮은순(코드길이)
    DEBUG("디버깅", true, false),        // 개인종료, 시간빠른순
    BLIND("블라인드", true, false),      // 개인종료
    RELAY("릴레이", false, true),        // 동시종료
    ROGUE("로그라이크", true, false),     // 개인종료(보스클리어)
    LOGIC_MAZE("로직 메이즈", true, false); // 개인종료, 점수낮은순(코스트)

    private final String description;
    private final boolean individualFinish;
    private final boolean highBetter;

    BattleMode(String description, boolean individualFinish, boolean highBetter) {
        this.description = description;
        this.individualFinish = individualFinish;
        this.highBetter = highBetter;
    }
}
