package com.passfail.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TierChangeReason {
	
	//배틀 관련 (데일리 티어)
	BATTLE_WIN  ("배틀 승리"),
    BATTLE_LOSE ("배틀 패배"),
    BATTLE_DRAW ("배틀 무승부"),
    
	//코딩 테스트 관련 (총 티어)
    PROBLEM_SOLVE_FIRST  ("문제 첫 번째 정답"),   // 높은 배점
    PROBLEM_SOLVE_RETRY  ("문제 재시도 정답"),     // 낮은 배점
    
	//시스템 관리자
    DAILY_RESET   ("데일리 초기화"),
    SEASON_RESET  ("시즌 초기화"),
    ADMIN_ADJUST  ("관리자 조정"),
    EVENT_BONUS   ("이벤트 보너스");
	
	private final String description;
	
}
