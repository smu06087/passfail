package com.passfail.codingtest.dto;

import lombok.*;

/**
 * 사용자가 테스트 목적으로 직접 추가한 테스트 케이스를 담는 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomTestCaseRequest {
    private String input;    // 입력값
    private String expected; // 기대 결과값
}
