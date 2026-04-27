package com.passfail.codingtest.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {
    private boolean success;
    private String output;
    private String error;
    private long executionTime;
    private long memoryUsed;
    private String status; // CORRECT, WRONG, TIMEOUT, MEMORY_LIMIT_EXCEEDED, RUNTIME_ERROR, COMPILE_ERROR
}
