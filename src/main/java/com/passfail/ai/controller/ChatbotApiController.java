package com.passfail.ai.controller;

import com.passfail.ai.dto.ChatSendRequest;
import com.passfail.ai.dto.ChatSendResponse;
import com.passfail.ai.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatbotApiController {

    private final ChatbotService chatbotService;

    @PostMapping("/send")
    public ResponseEntity<ChatSendResponse> send(@RequestBody ChatSendRequest request) {
        String answer = chatbotService.generateAnswer(request != null ? request.getMessage() : null);
        return ResponseEntity.ok(ChatSendResponse.builder().answer(answer).build());
    }
}
