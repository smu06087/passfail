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

		remove(userId);

		SseEmitter emitter = new SseEmitter(TIMEOUT);
		emitters.put(userId, emitter);

		// 연결 종료/에러 시 삭제 처리
		emitter.onCompletion(() -> {
			System.out.println("[SseService] subscribe onCompletion, emitter remove");
			remove(userId);
		});
		emitter.onTimeout(() -> {
			System.out.println("[SseService] subscribe onTimeout, emitter remove");
			remove(userId);
		});
		emitter.onError((e) -> {
			System.out.println("[SseService] subscribe onError, emitter remove");
			remove(userId);
		});

		// 최초 연결 확인용
		try {
			System.out.println("[SseService] subscribe success, send connection message");
			emitter.send(SseEmitter.event().name("connect").data("connected"));

			/*// 3초 뒤 공지 테스트 발송
			CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
				send(userId, "notice", "testMsg");
			});

			// 6초(공지 테스트 이후 3초) 뒤 공지 테스트 발송
			CompletableFuture.delayedExecutor(6, TimeUnit.SECONDS).execute(() -> {
				Map<String, Object> payload = new HashMap<>();
				payload.put("roomId", 333);
				payload.put("from", "tester");
				send(userId, "invite", payload);
			});*/

		} catch (IOException e) {
			remove(userId);
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
		} catch (IOException e) {
			System.out.println("[SseService] send fail IOException, eventName:" + eventName + " object:"
					+ data.toString() + " e:" + e.getMessage());
			emitter.complete();
			remove(userId);
		}

	}

	private void remove(Long userId) {

		System.out.println("[SseService] emitter remove, id:" + userId);

		SseEmitter old = emitters.remove(userId);
		if (old != null)
			old.complete();
	}

	// heartbeat (30초마다 ping)
	@Scheduled(fixedRate = 30000)
	public void heartbeat() {

		for (Long userId : emitters.keySet()) {
			send(userId, "ping", "keep-alive");
		}
	}
}
