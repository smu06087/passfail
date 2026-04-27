package com.passfail.problem.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResponse {
    private Long caseId;
    private String inputData;
    private String expectedOutput;
    private Boolean isSample;
}
