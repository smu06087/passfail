package com.passfail.battle.room;

import com.passfail.entity.BattleParticipantEntity;
import com.passfail.enums.BattleParticipantStatus;
import com.passfail.enums.BattleRoomStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class BattleWebSocketEventListener {

    private final BattleRoomService battleRoomService;
    private final BattleParticipantRepository battleParticipantRepository;
    private final BattleRoomRepository battleRoomRepository;
    
	@Autowired
	private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        // headers에서 roomId와 memberId 추출 (최초 연결 시)
        String roomIdStr = headerAccessor.getFirstNativeHeader("roomId");
        String memberIdStr = headerAccessor.getFirstNativeHeader("memberId");

        if (roomIdStr != null && memberIdStr != null) {
            Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put("roomId", Long.parseLong(roomIdStr));
                sessionAttributes.put("memberId", Long.parseLong(memberIdStr));
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes != null && sessionAttributes.containsKey("roomId") && sessionAttributes.containsKey("memberId")) {
            Long roomId = (Long) sessionAttributes.get("roomId");
            Long memberId = (Long) sessionAttributes.get("memberId");

            System.out.println("User " + memberId + " disconnected from room " + roomId);

            battleRoomRepository.findById(roomId).ifPresent(room -> {
                if (room.getStatus() == BattleRoomStatus.WAITING) {
                    // 대기 중일 때는 퇴장 처리
                    System.out.println("[BattleWebSocketEventListener] Room status is WAITING, calling leaveRoom for user " + memberId);
                    battleRoomService.leaveRoom(roomId, memberId);
                } else if (room.getStatus() == BattleRoomStatus.IN_PROGRESS) {
                    // 게임 중일 때는 상태만 DISCONNECTED로 변경 및 타임아웃 예약
                    System.out.println("[BattleWebSocketEventListener] Room status is IN_PROGRESS, calling handleDisconnect for user " + memberId);
                    battleRoomService.handleDisconnect(roomId, memberId);
                    
                    // 다른 유저들에게 연결 끊김 알림 (UI 표시용)
                    messagingTemplate.convertAndSend("/topic/room/" + roomId, Map.of(
                        "type", "status",
                        "message", "USER_DISCONNECTED",
                        "memberId", memberId
                    ));
                } else {
                    // STARTING 상태 등에서는 아무것도 하지 않음 (전환 기간 보장)
                    System.out.println("Room status is " + room.getStatus() + ", ignoring disconnect for user " + memberId);
                }
            });
        }
    }
}
