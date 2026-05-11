package com.passfail.ranking.dto;

import lombok.Data;

@Data
public class RankingRequestDTO {
    private Long memberId;
    private Long problemId;   // 푼 문제 ID
    private Integer difficulty; // 문제 난이도 (점수에 반영)
    private Double accuracy;   // 정확도
    private Long executionTime; // 소요 시간
}
