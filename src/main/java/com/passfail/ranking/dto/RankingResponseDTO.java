package com.passfail.ranking.dto;

import com.passfail.enums.Tier;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingResponseDTO {
    
    private Long memberId;
    
    /**
     * ⭐ nickname → username 으로 변경
     * MemberEntity의 실제 필드명이 username이므로
     */
    private String username;
    
    // 누적 점수 (문제풀이 + 게임 통합)
    private Integer totalScore;
    
    // 현재 순위
    private Integer currentRank;
    
    // 현재 티어 (Enum → JSON 변환 시 자동으로 문자열로 변환됨)
    private Tier tier;
    
    // 추가 정보 (선택사항)
    // private Integer problemSolveCount;  // 향후 추가 가능
    // private Integer gamePlayCount;      // 향후 추가 가능
}