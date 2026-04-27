package com.passfail.codingtest.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomTestCaseRequest {
    private String input;
    private String expected;
}
