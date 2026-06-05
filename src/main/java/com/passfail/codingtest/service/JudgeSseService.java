package com.passfail.codingtest.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 채점 진행 상황을 클라이언트에 실시간으로 전달하기 위한 SSE 서비스
 * submissionId(Long)와 runId(UUID String) 모두를 수용하기 위해 ID를 String으로 처리합니다.
 */
@Service
public class JudgeSseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 특정 작업 ID(submissionId 또는 runId)에 대한 SSE 연결 생성
     */
    public SseEmitter subscribe(String id) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃
        emitters.put(id, emitter);

        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
        emitter.onError((e) -> emitters.remove(id));

        try {
            emitter.send(SseEmitter.event().name("connect").data("Connected for ID: " + id));
        } catch (IOException e) {
            emitters.remove(id);
        }

        return emitter;
    }

    public void sendStatus(String id, String message) {
        SseEmitter emitter = emitters.get(id);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("status").data(message));
            } catch (IOException e) {
                emitters.remove(id);
            }
        }
    }

    public void complete(String id, String finalStatus) {
        SseEmitter emitter = emitters.get(id);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("complete").data(finalStatus));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            } finally {
                emitters.remove(id);
            }
        }
    }
}
