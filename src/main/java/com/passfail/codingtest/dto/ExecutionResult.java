package com.passfail.codingtest.dto;

import lombok.*;

/**
 * 코드 실행 결과를 담는 DTO
 * 개별 테스트 케이스의 성공 여부, 출력값, 에러 메시지, 실행 시간 등을 포함합니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {
    private boolean success;      // 통과 여부
    private String output;        // 실제 출력값
    private String error;         // 에러 발생 시 에러 메시지
    private long executionTime;   // 실행 시간 (ms)
    private long memoryUsed;      // 사용 메모리 (KB)
    private String status;        // 실행 상태 (CORRECT, WRONG, TIMEOUT, MEMORY_LIMIT_EXCEEDED, RUNTIME_ERROR, COMPILE_ERROR)
}
