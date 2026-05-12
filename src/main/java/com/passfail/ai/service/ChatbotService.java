package com.passfail.ai.service;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final OpenAiChatService openAiChatService;

    public String generateAnswer(String message) {
        if (!StringUtils.hasText(message)) {
            return "질문을 입력해 주시면 바로 답변할게요.";
        }

        String normalized = message.trim();

        if (isGreeting(normalized)) {
            return "안녕하세요. 무엇을 도와드릴까요?";
        }

        return openAiChatService.ask(Collections.emptyList(), normalized);
    }

    private boolean isGreeting(String message) {
        String compact = message.replaceAll("\\s+", "").toLowerCase();
        return compact.contains("안녕") ||
            compact.contains("안녕하세요") ||
            compact.contains("hello") ||
            compact.contains("hi");
    }
}
