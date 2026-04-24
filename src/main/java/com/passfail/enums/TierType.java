package com.passfail.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TierType {
	
	DAILY ("데일리 티어 - 배틀 기반"),
    TOTAL ("총 티어 - 문제 풀이 기반");
	
	private final String description;
	
}
