package com.passfail.ai.controller;

import com.passfail.ai.dto.AiChatResponse;
import com.passfail.ai.dto.AiChatSessionDetailResponse;
import com.passfail.ai.dto.AiChatSessionListResponse;
import com.passfail.ai.dto.AiSessionResponse;
import com.passfail.ai.service.AiQuestionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private static final String ANONYMOUS_USER = "anonymousUser";
    private static final String SESSION_IDS_KEY = "aiAllowedSessionIds";

    private final AiQuestionService aiQuestionService;

    @PostMapping(value = "/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AiSessionResponse> createSession(
        Authentication authentication,
        HttpSession httpSession,
        @RequestBody(required = false) Map<String, Object> ignored
    ) {
        try {
            String username = extractUsername(authentication);
            Long sessionId = aiQuestionService.createSession(username);

            if (username == null) {
                getAllowedSessionIds(httpSession).add(sessionId);
            }

            return ResponseEntity.ok(
                AiSessionResponse.builder()
                    .success(true)
                    .message("채팅방을 생성했습니다.")
                    .sessionId(sessionId)
                    .build()
            );
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                AiSessionResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build()
            );
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                AiSessionResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build()
            );
        }
    }

    @GetMapping(value = "/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AiChatSessionListResponse> getSessions(
        Authentication authentication,
        HttpSession httpSession
    ) {
        String username = extractUsername(authentication);
        return ResponseEntity.ok(
            AiChatSessionListResponse.builder()
                .success(true)
                .sessions(aiQuestionService.getSessions(username, getAllowedSessionIds(httpSession)))
                .build()
        );
    }

    @GetMapping(value = "/session/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AiChatSessionDetailResponse> getSessionDetail(
        Authentication authentication,
        HttpSession httpSession,
        @PathVariable("sessionId") Long sessionId
    ) {
        String username = extractUsername(authentication);

        if (username == null && !getAllowedSessionIds(httpSession).contains(sessionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                AiChatSessionDetailResponse.builder()
                    .success(false)
                    .sessionId(sessionId)
                    .build()
            );
        }

        try {
            return ResponseEntity.ok(aiQuestionService.getSessionDetail(username, sessionId));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                AiChatSessionDetailResponse.builder()
                    .success(false)
                    .sessionId(sessionId)
                    .build()
            );
        }
    }

    @PostMapping(
        value = "/chat",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AiChatResponse> chat(
        Authentication authentication,
        HttpSession httpSession,
        @RequestParam("sessionId") Long sessionId,
        @RequestParam(value = "content", required = false) String content,
        @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        String username = extractUsername(authentication);

        if (sessionId == null) {
            return ResponseEntity.badRequest().body(
                AiChatResponse.builder()
                    .success(false)
                    .message("채팅방 정보가 없습니다.")
                    .build()
            );
        }

        if (username == null && !getAllowedSessionIds(httpSession).contains(sessionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                AiChatResponse.builder()
                    .success(false)
                    .message("해당 채팅방에 접근할 수 없습니다.")
                    .build()
            );
        }

        if (username != null && !aiQuestionService.canAccessSession(username, sessionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                AiChatResponse.builder()
                    .success(false)
                    .message("해당 채팅방에 접근할 수 없습니다.")
                    .build()
            );
        }

        try {
            String answer = aiQuestionService.chat(username, sessionId, content, image);
            return ResponseEntity.ok(
                AiChatResponse.builder()
                    .success(true)
                    .message("응답을 생성했습니다.")
                    .answer(answer)
                    .build()
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                AiChatResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build()
            );
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AiChatResponse.builder()
                    .success(false)
                    .message("AI 응답 처리 중 오류가 발생했습니다.")
                    .build()
            );
        }
    }

    @DeleteMapping(value = "/session/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AiChatResponse> deleteSession(
        Authentication authentication,
        HttpSession httpSession,
        @PathVariable("sessionId") Long sessionId
    ) {
        String username = extractUsername(authentication);

        try {
            if (username == null) {
                if (!getAllowedSessionIds(httpSession).contains(sessionId)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        AiChatResponse.builder()
                            .success(false)
                            .message("로그인이 필요합니다.")
                            .build()
                    );
                }

                aiQuestionService.deleteAnonymousSession(sessionId);
                getAllowedSessionIds(httpSession).remove(sessionId);
            } else {
                aiQuestionService.deleteSession(username, sessionId);
            }

            return ResponseEntity.ok(
                AiChatResponse.builder()
                    .success(true)
                    .message("채팅방을 삭제했습니다.")
                    .build()
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                AiChatResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build()
            );
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                AiChatResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build()
            );
        }
    }

    private String extractUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (ANONYMOUS_USER.equals(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }

    @SuppressWarnings("unchecked")
    private Set<Long> getAllowedSessionIds(HttpSession httpSession) {
        Object value = httpSession.getAttribute(SESSION_IDS_KEY);
        if (value instanceof Set<?>) {
            return (Set<Long>) value;
        }

        Set<Long> sessionIds = new HashSet<>();
        httpSession.setAttribute(SESSION_IDS_KEY, sessionIds);
        return sessionIds;
    }
}
