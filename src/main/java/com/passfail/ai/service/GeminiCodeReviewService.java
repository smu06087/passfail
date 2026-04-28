package com.passfail.ai.service;

import com.passfail.codingtest.dto.ExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class GeminiCodeReviewService implements AiCodeReviewService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient.Builder webClientBuilder;

    @Override
    public String generateReview(String code, List<ExecutionResult> results) {
        if (apiKey == null || apiKey.equals("YOUR_API_KEY_HERE") || apiKey.isEmpty()) {
            return "⚠️ **AI 리뷰 안내**: 현재 시스템에 API 키가 설정되지 않았습니다. 관리자에게 문의해 주세요.";
        }

        long avgTime = (long) results.stream().mapToLong(ExecutionResult::getExecutionTime).average().orElse(0);
        boolean allCorrect = results.stream().allMatch(ExecutionResult::isSuccess);

        String prompt = String.format(
            "당신은 시니어 자바 개발자이자 기술 면접관입니다. 다음 사용자가 제출한 코드를 분석하고 전문적인 리뷰를 제공해 주세요.\n\n" +
            "### 제출 코드:\n```java\n%s\n```\n\n" +
            "### 실행 결과:\n- 성공 여부: %s\n- 평균 실행 시간: %dms\n\n" +
            "### 요청 사항:\n" +
            "1. **효율성 분석**: 시간 복잡도와 공간 복잡도를 분석하고 개선 방안을 제시하세요.\n" +
            "2. **코드 품질**: 변수 명명, 구조, 자바의 관례(Convention) 관점에서 피드백을 주세요.\n" +
            "3. **베스트 프랙티스**: 최신 자바(Java 21) 기능을 활용하거나 더 효율적인 알고리즘이 있다면 추천해 주세요.\n" +
            "4. **총평**: 학습에 도움이 될만한 격려와 함께 마무리해 주세요.\n\n" +
            "답변은 한국어로, Markdown 형식을 사용하여 가독성 있게 작성해 주세요.",
            code, allCorrect ? "전체 통과" : "일부 실패", avgTime
        );

        try {
            WebClient webClient = webClientBuilder.build();

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt)
                    ))
                )
            );

            Map<String, Object> response = webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (!parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }

            return "❌ AI 리뷰 생성 중 오류가 발생했습니다. (API 응답 형식 오류)";

        } catch (Exception e) {
            log.error("Gemini API Error", e);
            return "❌ AI 리뷰 서버와 통신 중 문제가 발생했습니다: " + e.getMessage();
        }
    }
}
