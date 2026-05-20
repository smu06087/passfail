package com.passfail.util.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {
	// userId -> emitter 여러개 (멀티탭 대응)
	private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
	private static final long TIMEOUT = 1000L * 60 * 60; // 1시간

	public SseEmitter subscribe(Long userId) {

		// 기존 연결이 있다면 명시적으로 종료 시도 (새 연결로 교체)
		SseEmitter oldEmitter = emitters.remove(userId);
		if (oldEmitter != null) {
			try {
				oldEmitter.complete();
			} catch (Exception e) {
				// 이미 종료된 경우 무시
			}
		}

		SseEmitter emitter = new SseEmitter(TIMEOUT);
		emitters.put(userId, emitter);

		// 연결 종료/에러 시 맵에서만 삭제 (이미 종료/에러 상태이므로 complete() 호출 불필요)
		emitter.onCompletion(() -> {
			System.out.println("[SseService] subscribe onCompletion, emitter remove");
			emitters.remove(userId);
		});
		emitter.onTimeout(() -> {
			System.out.println("[SseService] subscribe onTimeout, emitter remove");
			emitters.remove(userId);
		});
		emitter.onError((e) -> {
			System.out.println("[SseService] subscribe onError, emitter remove");
			emitters.remove(userId);
		});

		// 최초 연결 확인용
		try {
			System.out.println("[SseService] subscribe success, send connection message");
			emitter.send(SseEmitter.event().name("connect").data("connected"));
		} catch (IOException e) {
			emitters.remove(userId);
			System.out.println("[SseService] subscribe fail, " + e.getMessage());
		}

		return emitter;
	}

	public void send(Long userId, String eventName, Object data) {

		System.out.println("[SseService] send, eventName:" + eventName + " object:" + data.toString());

		SseEmitter emitter = emitters.get(userId);

		if (emitter == null) {
			System.out.println(
					"[SseService] send fail emitter == null, eventName:" + eventName + " object:" + data.toString());
			return;
		}

		try {
			emitter.send(SseEmitter.event().name(eventName).data(data));
		} catch (Exception e) {
			System.out.println("[SseService] send fail, eventName:" + eventName + " object:"
					+ data.toString() + " e:" + e.getMessage());
			// 전송 실패 시 맵에서 제거 (complete() 호출 시 예외 발생 가능성이 높으므로 지양)
			emitters.remove(userId);
		}

	}

	private void remove(Long userId) {
		System.out.println("[SseService] emitter remove, id:" + userId);
		emitters.remove(userId);
	}

	// heartbeat (30초마다 ping)
	@Scheduled(fixedRate = 30000)
	public void heartbeat() {

		for (Long userId : emitters.keySet()) {
			send(userId, "ping", "keep-alive");
		}
	}
}
