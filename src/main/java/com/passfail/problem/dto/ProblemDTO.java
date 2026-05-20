package com.passfail.problem.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ProblemDTO {
    private Long problemId;
    private Long createdBy;
    private String title;
    private String shortDescription;
    private String description;
    private String difficulty;
    private String category;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private String status;
    private Double acceptanceRate;
    private Integer submissionCount;
    private Integer acceptedCount;
    private boolean solved;
    private LocalDateTime createdAt;
    private List<String> tags = new ArrayList<>();
    private List<String> sampleInputs = new ArrayList<>();
    private List<String> sampleOutputs = new ArrayList<>();
    private List<String> testInputs = new ArrayList<>();
    private List<String> testOutputs = new ArrayList<>();
}
