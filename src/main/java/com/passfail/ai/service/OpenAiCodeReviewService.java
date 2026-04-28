package com.passfail.ai.service;

import com.passfail.codingtest.dto.ExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAiCodeReviewService implements AiCodeReviewService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.model}")
    private String model;

    private final WebClient.Builder webClientBuilder;

    @Override
    public String generateReview(String code, List<ExecutionResult> results) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "⚠️ **AI 리뷰 안내**: API 키가 설정되지 않았습니다.";
        }

        long avgTime = (long) results.stream().mapToLong(ExecutionResult::getExecutionTime).average().orElse(0);
        boolean allCorrect = results.stream().allMatch(ExecutionResult::isSuccess);

        String systemPrompt = "당신은 시니어 자바 개발자이자 기술 면접관입니다. 사용자의 코드를 분석하고 Markdown 형식으로 리뷰를 제공하세요.";
        String userPrompt = String.format(
            "### 제출 코드:\n```java\n%s\n```\n\n" +
            "### 실행 결과:\n- 성공 여부: %s\n- 평균 실행 시간: %dms\n\n" +
            "### 요청 사항:\n" +
            "1. **효율성 분석**: 시간/공간 복잡도 분석 및 개선 방안.\n" +
            "2. **코드 품질**: 변수 명명, 구조, 자바 관례 피드백.\n" +
            "3. **베스트 프랙티스**: Java 21 최신 기능 활용 제안.\n" +
            "4. **총평**: 격려의 메시지.\n\n" +
            "답변은 한국어로 작성해 주세요.",
            code, allCorrect ? "전체 통과" : "일부 실패", avgTime
        );

        try {
            WebClient webClient = webClientBuilder.build();

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.7
            );

            Map<String, Object> response = webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    return (String) message.get("content");
                }
            }

            return "❌ AI 리뷰 생성 중 오류가 발생했습니다. (OpenAI 응답 오류)";

        } catch (Exception e) {
            log.error("OpenAI API Error", e);
            return "❌ AI 리뷰 서버와 통신 중 문제가 발생했습니다: " + e.getMessage();
        }
    }
}
