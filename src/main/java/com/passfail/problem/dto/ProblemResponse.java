package com.passfail.problem.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemResponse {
    private Long problemId;
    private String title;
    private String description;
    private String difficulty;
    private String category;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private Double acceptanceRate;
    private List<TestCaseResponse> testCases;
}
