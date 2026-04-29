package com.passfail.problem.dto;

import lombok.Data;

@Data
public class ProblemTagDTO {
    private Long tagId;
    private Long problemId;
    private String tagName;
}
