package com.passfail.ai.service;

import com.passfail.codingtest.dto.ExecutionResult;
import java.util.List;

public interface AiCodeReviewService {
    /**
     * 사용자가 작성한 코드와 실행 결과를 분석하여 AI 리뷰를 생성합니다.
     * 
     * @param code 사용자가 제출한 자바 소스 코드
     * @param results 코드 실행 결과 (실행 시간, 성공 여부 등)
     * @return AI 분석 결과 (Markdown 형식)
     */
    String generateReview(String code, List<ExecutionResult> results);
}
