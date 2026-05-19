package com.passfail.enums;

import lombok.Getter;

@Getter
public enum BattleMode {
    QUICK("빨리 풀기"),
    MANY("많이 풀기"),
    SURVIVAL("서바이벌"),
    GOLF("코드 골프"),
    DEBUG("디버깅"),
    BLIND("블라인드"),
    RELAY("릴레이"),
    ROGUE("로그라이크"),
    LOGIC_MAZE("로직 메이즈");

    private final String description;

    BattleMode(String description) {
        this.description = description;
    }
}
