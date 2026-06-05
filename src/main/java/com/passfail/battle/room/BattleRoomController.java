package com.passfail.battle.room;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.passfail.entity.BattleChatEntity;
import com.passfail.entity.BattleParticipantEntity;
import com.passfail.entity.BattleRoomEntity;
import com.passfail.entity.MemberEntity;
import com.passfail.enums.BattleParticipantStatus;
import com.passfail.enums.BattleRoomStatus;
import com.passfail.enums.Difficulty;
import com.passfail.member.repository.MemberRepository;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/battle/room")
public class BattleRoomController {

    private final BattleParticipantRepository battleParticipantRepository;
    private final MemberRepository memberRepository;
    private final BattleRoomRepository battleRoomRepository;
    private final BattleCompetitionPermissionRepository battleCompetitionPermissionRepository;

	@Autowired
	private BattleRoomService battleRoomService;

	@Autowired
	private com.passfail.battle.mode.InteractiveMazeService interactiveMazeService;

	
	// 방 리스트
	@GetMapping("/lobby")
	public String list(@RequestParam(value = "search", required = false) String search,
					   @RequestParam(value = "enterableOnly", defaultValue = "false") boolean enterableOnly,
					   Principal principal, Model model) {
		System.out.println("lobby search: " + search + ", enterableOnly: " + enterableOnly);
		
		List<BattleRoomDTO> roomDtoList = battleRoomService.getRoomList(search, enterableOnly, 0, 15);
		model.addAttribute("rooms", roomDtoList);
		model.addAttribute("search", search);
		model.addAttribute("enterableOnly", enterableOnly);

		if (principal != null) {
			model.addAttribute("currentUsername", principal.getName());
		}

		return "battle/room/lobby";
	}

	// 빠른 매칭
	@PostMapping("/quick-match")
	public String quickMatch(@RequestParam("mode") String mode, 
							 @RequestParam("difficulty") String difficulty, 
							 Principal principal) {
		if (principal == null) return "redirect:/login";

		String username = principal.getName();
		MemberEntity member = memberRepository.findByUsername(username).orElse(null);
		if (member == null) return "redirect:/login";

		Long roomId = battleRoomService.findQuickMatch(member.getMemberId(), mode, difficulty);
		
		if (roomId == null) {
			return "redirect:/battle/room/lobby?error=no_match_found";
		}
		
		return "redirect:/battle/room/join/" + roomId;
	}

	// 방 생성
	@GetMapping("/create")
	public String create(Principal principal, Model model) {
		System.out.println("create");
		
		if (principal != null) {
			MemberEntity member = memberRepository.findByUsername(principal.getName()).orElse(null);
			if (member != null) {
				boolean canCreateCompetition = battleCompetitionPermissionRepository.findByMemberId(member.getMemberId()).isPresent();
				model.addAttribute("canCreateCompetition", canCreateCompetition);
				model.addAttribute("username", principal.getName());
				model.addAttribute("currentUsername", principal.getName());
			}
		}
		
		return "battle/room/create";
	}
	
	@PostMapping("/create")
	public String postCreate(@RequestParam("roomName") String roomName, 
			@RequestParam("maxUserCount") int maxUserCount, @RequestParam("difficalty") String difficalty, 
			@RequestParam(value = "password", defaultValue = "") String password,
			@RequestParam(value = "tags", defaultValue = "") String tags,
			@RequestParam(value = "mode", defaultValue = "QUICK") String mode,
			@RequestParam(value = "startAt", required = false) String startAtStr,
			@RequestParam(value = "endAt", required = false) String endAtStr,
			Principal principal) {
		
		if (principal == null) return "redirect:/login";

		MemberEntity member = memberRepository.findByUsername(principal.getName()).orElse(null);
		if (member == null) return "redirect:/login";
		
		Long hostId = member.getMemberId();

		LocalDateTime startAt = null;
		LocalDateTime endAt = null;
		
		if (startAtStr != null && !startAtStr.isEmpty()) {
			startAt = LocalDateTime.parse(startAtStr);
		}
		if (endAtStr != null && !endAtStr.isEmpty()) {
			endAt = LocalDateTime.parse(endAtStr);
		}

		Long roomId = battleRoomService.createRoom(hostId, roomName, password, maxUserCount, Difficulty.valueOf(difficalty), tags, mode, startAt, endAt);	
		
		return "redirect:/battle/room/join/" + roomId;
	}

	@PostMapping("/join/verify")
	public String postJoinVerify(@RequestParam("roomId") Long roomId, 
			@RequestParam("password") String password, Principal principal, Model model) {
		
		if (principal == null) return "redirect:/login";
		String username = principal.getName();
		MemberEntity member = memberRepository.findByUsername(username).orElse(null);
		if (member == null) return "redirect:/login";

		BattleRoomEntity room = battleRoomRepository.findById(roomId).orElse(null);
		if (room == null) return "redirect:/battle/room/lobby";

		if (room.getPassword() != null && !room.getPassword().isEmpty()) {
			if (!room.getPassword().equals(password)) {
				// TODO: 비밀번호 틀림 알림 처리
				return "redirect:/battle/room/lobby?error=wrong_password";
			}
		}

		battleRoomService.joinRoom(roomId, member.getMemberId());
		
		return "redirect:/battle/room/join/" + roomId;
	}

	// 방 입장화면
	@GetMapping("/join/{roomid}")
	public String room(@PathVariable("roomid") String roomid, Principal principal, Model model) {
		if (principal == null) return "redirect:/login";
		Long roomId = Long.parseLong(roomid);
		
		String username = principal.getName();
		MemberEntity member = memberRepository.findByUsername(username).orElse(null);
		
		if (member == null) {
			return "redirect:/login";
		}

		BattleRoomEntity room = battleRoomRepository.findById(roomId).orElse(null);
		if (room == null) {
			return "redirect:/battle/room/lobby";
		}
		
		// 1. 이미 시작된 방인 경우, 참여자 여부에 따라 분기
		if (room.getStatus() == BattleRoomStatus.IN_PROGRESS || room.getStatus() == BattleRoomStatus.STARTING) {
			boolean isAlreadyParticipant = battleParticipantRepository.findByRoomIdAndMemberId(roomId, member.getMemberId()).isPresent();
			if (isAlreadyParticipant) {
				// 재접속: 바로 대결 화면으로 유도
				Long seed = room.getBattleSeed();
				if (room.getBattleMode() == com.passfail.enums.BattleMode.ROGUE) {
					return "redirect:/battle/mode/rogueMap?roomId=" + roomId + "&seed=" + seed;
				} else {
					return "redirect:/battle/mode/editor?roomId=" + roomId + "&seed=" + seed;
				}
			} else {
				// 참여자가 아닌데 들어온 경우 (lobby에서 막았지만 URL 직접 입력 대응)
				return "redirect:/battle/room/lobby?error=already_started";
			}
		}

		// 2. 대기 중인 방 입장 로직 (기존)
		List<BattleParticipantEntity> participants = battleParticipantRepository.findByRoomIdWithMember(roomId);
		boolean isParticipant = participants.stream().anyMatch(p -> p.getMemberId().equals(member.getMemberId()));
		if (!isParticipant) {
			battleRoomService.joinRoom(roomId, member.getMemberId());
			participants = battleParticipantRepository.findByRoomIdWithMember(roomId); // 갱신
		}

		boolean isHost = room.getHostId().equals(member.getMemberId());
		boolean isCompetition = room.getStatus() == BattleRoomStatus.WAITING && room.getStartAt() != null; 

		model.addAttribute("roomId", roomId);
		model.addAttribute("username", username);
		model.addAttribute("currentUserId", member.getMemberId());
		model.addAttribute("hostId", room.getHostId());
		model.addAttribute("participants", participants);
		model.addAttribute("maxParticipants", room.getMaxParticipants()); // 추가
		model.addAttribute("isHost", isHost);
		model.addAttribute("isCompetition", isCompetition); 
		
		return "battle/room/room";
	}
	
	@PostMapping("/start")
	public String postStart(@RequestParam("roomId") Long roomId, Model model) {
		System.out.println("start");		
		
		Long seed = battleRoomService.startBattle(roomId);
		if (seed == null) {
			return "redirect:/battle/room/join/" + roomId;
		}

		BattleRoomEntity room = battleRoomRepository.findById(roomId).orElseThrow();
		
		if (room.getBattleMode() == com.passfail.enums.BattleMode.ROGUE) {
			return "redirect:/battle/mode/rogueMap?roomId=" + roomId + "&seed=" + seed;
		} else {
			return "redirect:/battle/mode/editor?roomId=" + roomId + "&seed=" + seed;
		}
	}

	
	//WS Request
	@MessageMapping("/chat2/send") // 클라이언트가 /app/chat/send로 보낼 때
	@SendTo("/topic/chat2")       // /topic/chat을 구독 중인 모두에게 전달
	public BattleChatEntity chat(BattleChatEntity message) {
		
	    return message;
	}
	
	private final SimpMessagingTemplate messagingTemplate;
	  
    @MessageMapping("/chat/send")
    public void send(BattleChatEntity msg) {
    	String senderName = "Unknown";
    	if (msg.getMemberId() != null) {
    		senderName = memberRepository.findById(msg.getMemberId())
    				.map(MemberEntity::getUsername).orElse("Unknown");
    	}
    	
        messagingTemplate.convertAndSend(
                "/topic/room/" + msg.getRoomId(),
                Map.of(
                        "type", "chat",
                        "message", msg.getMessage(),
                        "sender", senderName
                )
        );
    }

    @MessageMapping("/room/status")
    public void status(BattleChatEntity msg) {
        String statusMessage = msg.getMessage();
        Long roomId = msg.getRoomId();
        Long memberId = msg.getMemberId();
        
        if ("READY".equals(statusMessage)) {
            battleRoomService.updateParticipantStatus(roomId, memberId, BattleParticipantStatus.READY);
        } else if ("UNREADY".equals(statusMessage)) {
            battleRoomService.updateParticipantStatus(roomId, memberId, BattleParticipantStatus.WAITING);
        } else if ("START_GAME".equals(statusMessage)) {
            // 방장 권한 확인 로직 (옵션: 나중에 추가)
            
            // 1. 서버 상태를 먼저 STARTING으로 변경 (DisconnectListener 대응)
            Long seed = battleRoomService.startBattle(roomId);
            
            // 2. 변경된 상태와 Seed를 모든 유저에게 브로드캐스트
            battleRoomService.broadcastGameStart(roomId, seed);
        }
    }

    @MessageMapping("/room/confirm")
    public void confirm(BattleChatEntity msg) {
        Long roomId = msg.getRoomId();
        Long memberId = msg.getMemberId();
        battleRoomService.confirmEntry(roomId, memberId);
    }

    @MessageMapping("/room/position")
    public void position(BattleChatEntity msg) {
        Long roomId = msg.getRoomId();
        Long memberId = msg.getMemberId();
        String nodeId = msg.getMessage();
        
        boolean isCleared = false;
        if (nodeId.contains(":")) {
            String[] parts = nodeId.split(":");
            nodeId = parts[0];
            isCleared = Boolean.parseBoolean(parts[1]);
        }

        // 실시간 위치 브로드캐스트 및 DB 동기화 (간이 점수/경로 정보는 생략)
        battleRoomService.updateRogueProgress(roomId, memberId, nodeId, isCleared, 0, null, null);
    }

    @MessageMapping("/room/progress")
    public void updateProgress(BattleChatEntity msg) {
        String data = msg.getMessage();
        Long roomId = msg.getRoomId();
        Long memberId = msg.getMemberId();

        // 1. DB 상태 업데이트 로직 추가
        if (data.contains(":")) {
            String[] parts = data.split(":");
            String value = parts[0];
            String statusStr = parts[1];

            if ("FINISHED".equals(statusStr)) {
                // 최종 점수(코스트)와 함께 완료 처리
                int score = Integer.parseInt(value);
                battleRoomService.updateParticipantStatus(roomId, memberId, com.passfail.enums.BattleParticipantStatus.FINISHED, score);
            } else if ("EXITED".equals(statusStr)) {
                // 명시적 기권/퇴장 처리
                battleRoomService.updateParticipantStatus(roomId, memberId, com.passfail.enums.BattleParticipantStatus.EXITED, null);
            }
        }

        // 2. 다른 유저들에게 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/room/" + roomId,
            Map.of(
                "type", "progress",
                "memberId", memberId,
                "data", data
            )
        );
    }

    // [Interactive Maze Run] 사용자가 보낸 코드를 실시간 스트리밍 실행
    @MessageMapping("/maze/run")
    public void runMazeCode(Map<String, Object> payload) {
        Long roomId = Long.valueOf(payload.get("roomId").toString());
        Long memberId = Long.valueOf(payload.get("memberId").toString());
        String code = (String) payload.get("code");
        String mapData = (String) payload.get("mapData");
        String langStr = (String) payload.getOrDefault("language", "JAVA");
        com.passfail.enums.ProgrammingLanguage language = com.passfail.enums.ProgrammingLanguage.valueOf(langStr.toUpperCase());
        
        interactiveMazeService.startExecution(roomId, memberId, code, mapData, language);
    }

    @MessageMapping("/maze/control")
    public void controlMazeCode(Map<String, Object> payload) {
        Long roomId = Long.valueOf(payload.get("roomId").toString());
        Long memberId = Long.valueOf(payload.get("memberId").toString());
        String action = (String) payload.get("action");
        
        interactiveMazeService.handleControl(roomId, memberId, action);
    }

    @GetMapping("/{roomId}/positions")
    @ResponseBody
    public List<Map<String, Object>> getPositions(@PathVariable("roomId") Long roomId) {
        return battleRoomService.getAllParticipantPositions(roomId);
    }

    @GetMapping("/{roomId}/check-participant")
    @ResponseBody
    public Map<String, Object> checkParticipant(@PathVariable("roomId") Long roomId, Principal principal) {
        if (principal == null) return Map.of("isParticipant", false);
        MemberEntity member = memberRepository.findByUsername(principal.getName()).orElse(null);
        if (member == null) return Map.of("isParticipant", false);
        
        boolean isParticipant = battleParticipantRepository.findByRoomIdAndMemberId(roomId, member.getMemberId()).isPresent();
        return Map.of("isParticipant", isParticipant);
    }

    @PostMapping("/{roomId}/leave-forced")
    @ResponseBody
    public Map<String, Object> leaveForced(@PathVariable("roomId") Long roomId, Principal principal) {
        if (principal == null) return Map.of("success", false);
        MemberEntity member = memberRepository.findByUsername(principal.getName()).orElse(null);
        if (member == null) return Map.of("success", false);
        
        battleRoomService.leaveRoom(roomId, member.getMemberId());
        return Map.of("success", true);
    }

    @PostMapping("/{roomId}/rogue-update")
    @ResponseBody
    public Map<String, Object> updateRogue(@PathVariable("roomId") Long roomId, 
                                           @RequestBody Map<String, Object> payload,
                                           Principal principal) {
        if (principal == null) return Map.of("success", false);
        MemberEntity member = memberRepository.findByUsername(principal.getName()).orElseThrow();
        
        String nodeId = (String) payload.get("nodeId");
        boolean isCleared = (boolean) payload.get("isCleared");
        int scoreGain = (int) payload.get("scoreGain");
        String visitedNodesJson = (String) payload.get("visitedNodes");
        String visitedPathsJson = (String) payload.get("visitedPaths");

        battleRoomService.updateRogueProgress(roomId, member.getMemberId(), nodeId, isCleared, scoreGain, visitedNodesJson, visitedPathsJson);
        return Map.of("success", true);
    }
}

@Data
class ChatMessage {
    private Long roomId;
    private String message;
}
