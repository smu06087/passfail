package com.passfail.battle.room;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.passfail.util.connection.SseService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.passfail.entity.BattleParticipantEntity;
import com.passfail.entity.BattleRogueProgressEntity;
import com.passfail.entity.BattleRoomEntity;
import com.passfail.entity.MemberEntity;
import com.passfail.enums.BattleParticipantStatus;
import com.passfail.enums.BattleRoomStatus;
import com.passfail.enums.Difficulty;
import com.passfail.member.repository.MemberRepository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class BattleRoomService {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	@Autowired
    private SseService sseService;

	@Autowired
	private BattleParticipantRepository battleParticipantRepository;

	@Autowired
	private BattleRoomRepository battleRoomRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private BattleRogueProgressRepository battleRogueProgressRepository;

	@Autowired
	private com.passfail.problem.repository.ProblemRepository problemRepository;

	@Transactional
	public void updateParticipantStatus(Long roomId, Long memberId, com.passfail.enums.BattleParticipantStatus status, Integer score) {
		battleParticipantRepository.findByRoomIdAndMemberId(roomId, memberId).ifPresent(p -> {
			p.setStatus(status);
			if (score != null) {
				p.setScore(score);
				p.setFinishedAt(LocalDateTime.now());
			}
			battleParticipantRepository.save(p);
			
			BattleRoomEntity room = battleRoomRepository.findById(roomId).orElse(null);
			if (room != null && status == com.passfail.enums.BattleParticipantStatus.FINISHED && room.getBattleMode().isIndividualFinish()) {
				// 개인 종료 모드인 경우 현재 순위 계산 및 알림
				List<BattleParticipantEntity> participants = battleParticipantRepository.findByRoomId(roomId);
				List<BattleParticipantEntity> finished = participants.stream()
					.filter(bp -> bp.getStatus() == com.passfail.enums.BattleParticipantStatus.FINISHED)
					.sorted((p1, p2) -> compareParticipants(p1, p2, room.getBattleMode()))
					.toList();
				
				int currentRank = 0;
				for (int i = 0; i < finished.size(); i++) {
					if (finished.get(i).getMemberId().equals(memberId)) {
						currentRank = i + 1;
						break;
					}
				}
				
				List<Map<String, Object>> finishers = finished.stream()
					.map(bp -> Map.<String, Object>of(
						"username", bp.getMember() != null ? bp.getMember().getUsername() : "Unknown",
						"score", bp.getScore(),
						"finishedAt", bp.getFinishedAt()
					)).toList();

				messagingTemplate.convertAndSend("/topic/room/" + roomId, Map.of(
					"type", "individual_result",
					"memberId", memberId,
					"rank", currentRank,
					"score", score != null ? score : 0,
					"finishers", finishers,
					"totalParticipants", participants.size()
				));
			}
			
			// 모든 유저가 완료/퇴장했는지 확인하여 정산 트리거
			checkAndSettle(roomId);
		});
	}

	private int compareParticipants(BattleParticipantEntity p1, BattleParticipantEntity p2, com.passfail.enums.BattleMode mode) {
		if (p1.getScore() == null || p2.getScore() == null) return 0;
		if (!p1.getScore().equals(p2.getScore())) {
			if (mode.isHighBetter()) {
				return p2.getScore().compareTo(p1.getScore()); // 높은게 위로
			} else {
				return p1.getScore().compareTo(p2.getScore()); // 낮은게 위로
			}
		}
		// 점수 같으면 빨리 끝낸 사람이 위로
		if (p1.getFinishedAt() != null && p2.getFinishedAt() != null) {
			return p1.getFinishedAt().compareTo(p2.getFinishedAt());
		}
		return 0;
	}

	@Transactional
	public void forceSettle(Long roomId) {
		battleRoomRepository.findById(roomId).ifPresent(room -> {
			if (room.getStatus() == BattleRoomStatus.IN_PROGRESS) {
				List<BattleParticipantEntity> participants = battleParticipantRepository.findByRoomId(roomId);
				// 미완료자들도 현재 상태로 정산에 포함 (기권/연결끊김 제외)
				for (BattleParticipantEntity p : participants) {
					if (p.getStatus() == com.passfail.enums.BattleParticipantStatus.PLAYING) {
						p.setStatus(com.passfail.enums.BattleParticipantStatus.FINISHED);
						if (p.getScore() == null) p.setScore(0);
						p.setFinishedAt(LocalDateTime.now());
						battleParticipantRepository.save(p);
					}
				}
				settleBattle(room, participants);
			}
		});
	}

	@Transactional
	public void checkAndSettle(Long roomId) {
		battleRoomRepository.findById(roomId).ifPresent(room -> {
			if (room.getStatus() != BattleRoomStatus.IN_PROGRESS) return;

			List<BattleParticipantEntity> participants = battleParticipantRepository.findByRoomId(roomId);
			boolean allDone = participants.stream().allMatch(p -> 
				p.getStatus() == com.passfail.enums.BattleParticipantStatus.FINISHED || 
				p.getStatus() == com.passfail.enums.BattleParticipantStatus.EXITED ||
				p.getStatus() == com.passfail.enums.BattleParticipantStatus.DISCONNECTED
			);

			if (allDone) {
				settleBattle(room, participants);
			}
		});
	}

	private void settleBattle(BattleRoomEntity room, List<BattleParticipantEntity> participants) {
		// 정산 로직: 모드별 점수 방향성 + 시간순
		List<BattleParticipantEntity> sorted = participants.stream()
			.filter(p -> p.getStatus() == com.passfail.enums.BattleParticipantStatus.FINISHED)
			.sorted((p1, p2) -> compareParticipants(p1, p2, room.getBattleMode()))
			.toList();

		for (int i = 0; i < sorted.size(); i++) {
			sorted.get(i).setFinalRank(i + 1);
		}
		
		room.setStatus(BattleRoomStatus.FINISHED);
		battleRoomRepository.save(room);
		
		List<Map<String, Object>> results = sorted.stream().map(p -> Map.<String, Object>of(
			"username", p.getMember() != null ? p.getMember().getUsername() : "Unknown",
			"score", p.getScore(),
			"rank", p.getFinalRank(),
			"memberId", p.getMemberId()
		)).toList();

		// 결과 브로드캐스트
		messagingTemplate.convertAndSend("/topic/room/" + room.getRoomId(), Map.of(
			"type", "status",
			"message", "SETTLED",
			"results", results
		));
	}

	@Transactional
	public void handleDisconnect(Long roomId, Long memberId) {
		battleParticipantRepository.findByRoomIdAndMemberId(roomId, memberId).ifPresent(p -> {
			p.setStatus(com.passfail.enums.BattleParticipantStatus.DISCONNECTED);
			battleParticipantRepository.save(p);
			
			// 다른 유저들이 모두 종료되었는지 확인
			List<BattleParticipantEntity> others = battleParticipantRepository.findByRoomId(roomId).stream()
				.filter(bp -> !bp.getMemberId().equals(memberId)).toList();
			
			boolean othersDone = others.stream().allMatch(bp -> 
				bp.getStatus() == com.passfail.enums.BattleParticipantStatus.FINISHED || 
				bp.getStatus() == com.passfail.enums.BattleParticipantStatus.EXITED
			);

			long delay = othersDone ? 30 : 180; // 초 단위
			scheduler.schedule(() -> forceExitIfDisconnected(roomId, memberId), delay, TimeUnit.SECONDS);
		});
	}

	@Transactional
	public void forceExitIfDisconnected(Long roomId, Long memberId) {
		battleParticipantRepository.findByRoomIdAndMemberId(roomId, memberId).ifPresent(p -> {
			if (p.getStatus() == com.passfail.enums.BattleParticipantStatus.DISCONNECTED) {
				p.setStatus(com.passfail.enums.BattleParticipantStatus.EXITED);
				battleParticipantRepository.save(p);
				checkAndSettle(roomId);
			}
		});
	}

	public List<BattleRoomDTO> getRoomList(String searchQuery, boolean enterableOnly, int page, int size) {
		String query = null;
		String tag = null;
		if (searchQuery != null && !searchQuery.isEmpty()) {
			if (searchQuery.contains("#")) {
				String[] parts = searchQuery.split("#", 2);
				query = parts[0].trim().isEmpty() ? null : parts[0].trim();
				tag = parts[1].trim();
			} else {
				query = searchQuery.trim();
			}
		}
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "roomId"));
		Page<BattleRoomEntity> roomEntities = battleRoomRepository.searchRooms(query, tag, enterableOnly, pageable);
		return roomEntities.stream().map(this::convertToDTO).toList();
	}

	private BattleRoomDTO convertToDTO(BattleRoomEntity entity) {
		BattleRoomDTO dto = new BattleRoomDTO();
		BeanUtils.copyProperties(entity, dto);
		dto.setRoomName(entity.getRoomName());
		dto.setDifficulty(entity.getDifficulty() != null ? entity.getDifficulty().name() : "EASY");
		dto.setHasPassword(entity.getPassword() != null && !entity.getPassword().isEmpty());
		dto.setCurrentParticipantsCount(entity.getParticipants() != null ? entity.getParticipants().size() : 0);
		dto.setTags(entity.getTags());
		dto.setBattleMode(entity.getBattleMode());
		return dto;
	}

	@Transactional
	public Long findQuickMatch(Long userId, String mode, String difficulty) {
		List<BattleRoomEntity> candidates = battleRoomRepository.findQuickMatchCandidates();
		MemberEntity user = memberRepository.findById(userId).orElseThrow();
		int userScore = user.getTotalScore() != null ? user.getTotalScore() : 0;
		List<BattleRoomEntity> filtered = candidates.stream()
				.filter(r -> "ALL".equalsIgnoreCase(difficulty) || r.getDifficulty().name().equalsIgnoreCase(difficulty))
				.filter(r -> "ALL".equalsIgnoreCase(mode) || r.getBattleMode().name().equalsIgnoreCase(mode))
				.toList();
		if (filtered.isEmpty()) return null;
		List<BattleRoomEntity> sorted = filtered.stream()
				.sorted((r1, r2) -> {
					double avg1 = r1.getParticipants().stream().mapToInt(p -> p.getMember() != null ? p.getMember().getTotalScore() : 0).average().orElse(0);
					double avg2 = r2.getParticipants().stream().mapToInt(p -> p.getMember() != null ? p.getMember().getTotalScore() : 0).average().orElse(0);
					return Double.compare(Math.abs(avg1 - userScore), Math.abs(avg2 - userScore));
				})
				.limit(5).toList();
		return sorted.get((int) (Math.random() * sorted.size())).getRoomId();
	}

	@Transactional
	public Long createRoom(Long hostId, String roomName, String password, int maxUserCount, Difficulty difficalty, String tags, String mode, LocalDateTime startAt, LocalDateTime endAt) {
		if (password == null) password = "";
		com.passfail.enums.BattleMode bMode = com.passfail.enums.BattleMode.valueOf(mode.toUpperCase());
		Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "roomId"));
		Page<BattleRoomEntity> roomEntities = battleRoomRepository.findByStatus(BattleRoomStatus.FINISHED, pageable);
		BattleRoomEntity entity;
		if (roomEntities.isEmpty()) {
			entity = BattleRoomEntity.builder().hostId(hostId).roomName(roomName).password(password)
					.maxParticipants(maxUserCount).difficulty(difficalty).battleSeed(0L)
					.startAt(startAt).endAt(endAt).status(BattleRoomStatus.WAITING).tags(tags).battleMode(bMode).build();
		} else {
			Long roomid = roomEntities.getContent().get(0).getRoomId();
			// 방 재사용 시 이전 배틀의 로그 진행 데이터 완전 삭제
			battleRogueProgressRepository.deleteByRoomId(roomid);
			
			entity = BattleRoomEntity.builder().roomId(roomid).hostId(hostId).roomName(roomName).password(password)
					.maxParticipants(maxUserCount).difficulty(difficalty).battleSeed(0L)
					.startAt(startAt).endAt(endAt).status(BattleRoomStatus.WAITING).tags(tags).battleMode(bMode).build();
		}
		entity = battleRoomRepository.save(entity);
		joinRoom(entity.getRoomId(), hostId);
		return entity.getRoomId();
	}

	public Long startBattle(Long roomId) {
		battleRoomRepository.findById(roomId).ifPresent(entity -> {
			entity.setBattleSeed((long) (Math.random() * 1000000));
			entity.setStatus(BattleRoomStatus.STARTING);
			
			// ROGUE 및 LOGIC_MAZE 모드가 아닌 경우 문제 자동 선택
			if (entity.getBattleMode() != com.passfail.enums.BattleMode.ROGUE &&
				entity.getBattleMode() != com.passfail.enums.BattleMode.LOGIC_MAZE) {
				List<com.passfail.entity.ProblemEntity> problems = problemRepository.findByDifficulty(entity.getDifficulty());
				if (!problems.isEmpty()) {
					int randomIndex = (int) (Math.random() * problems.size());
					entity.setProblemId(problems.get(randomIndex).getProblemId());
				}
			}
			
			battleRoomRepository.save(entity);
			scheduler.schedule(() -> forceStartIfTimeout(roomId), 10, TimeUnit.SECONDS);

			// 종료 시간이 설정된 경우 자동 정산 예약
			if (entity.getEndAt() != null) {
				long delaySeconds = java.time.Duration.between(LocalDateTime.now(), entity.getEndAt()).toSeconds();
				if (delaySeconds > 0) {
					scheduler.schedule(() -> forceSettle(roomId), delaySeconds, TimeUnit.SECONDS);
				}
			}
		});
		return battleRoomRepository.findById(roomId).map(BattleRoomEntity::getBattleSeed).orElse(0L);
	}

	@Transactional
	public void forceStartIfTimeout(Long roomId) {
		battleRoomRepository.findById(roomId).ifPresent(room -> {
			if (room.getStatus() == BattleRoomStatus.STARTING) {
				room.setStatus(BattleRoomStatus.IN_PROGRESS);
				room.setActualStartedAt(LocalDateTime.now());
				battleRoomRepository.save(room);
				List<BattleParticipantEntity> participants = battleParticipantRepository.findByRoomId(roomId);
				for (BattleParticipantEntity p : participants) {
					if (p.getStatus() != BattleParticipantStatus.PLAYING) {
						p.setStatus(BattleParticipantStatus.DISCONNECTED);
						battleParticipantRepository.save(p);
					}
				}
				messagingTemplate.convertAndSend("/topic/room/" + roomId, Map.of("type", "game_sync", "message", "FORCE_START"));
			}
		});
	}

	@Transactional
	public void confirmEntry(Long roomId, Long userId) {
		List<BattleParticipantEntity> participants = battleParticipantRepository.findByRoomId(roomId);
		boolean allEntered = true;
		for (BattleParticipantEntity p : participants) {
			if (p.getMemberId().equals(userId)) {
				p.setStatus(BattleParticipantStatus.PLAYING);
				battleParticipantRepository.save(p);
			}
			if (p.getStatus() != BattleParticipantStatus.PLAYING) allEntered = false;
		}
		if (allEntered) {
			battleRoomRepository.findById(roomId).ifPresent(room -> {
				if (room.getStatus() == BattleRoomStatus.STARTING) {
					room.setStatus(BattleRoomStatus.IN_PROGRESS);
					if (room.getActualStartedAt() == null) room.setActualStartedAt(LocalDateTime.now());
					battleRoomRepository.save(room);
					messagingTemplate.convertAndSend("/topic/room/" + roomId, Map.of("type", "game_sync", "message", "ALL_READY"));
				}
			});
		}
	}

	@Transactional
	public void joinRoom(Long roomId, Long userId) {
		List<BattleParticipantEntity> existing = battleParticipantRepository.findByRoomId(roomId);
		if (existing.stream().anyMatch(p -> p.getMemberId().equals(userId))) return;
		List<BattleParticipantEntity> otherRooms = battleParticipantRepository.findByMemberId(userId);
		for (BattleParticipantEntity e : otherRooms) leaveRoom(e.getRoomId(), e.getMemberId());
		
		BattleRoomEntity room = battleRoomRepository.findById(roomId).orElseThrow();
		List<Integer> occupiedSlots = existing.stream().map(BattleParticipantEntity::getSlotIndex).toList();
		int assignedSlot = -1;
		for (int i = 0; i < room.getMaxParticipants(); i++) { if (!occupiedSlots.contains(i)) { assignedSlot = i; break; } }
		if (assignedSlot == -1) assignedSlot = occupiedSlots.size();
		
		MemberEntity member = memberRepository.findById(userId).orElse(null);
		BattleParticipantEntity entity = BattleParticipantEntity.builder().roomId(roomId).room(room).memberId(userId).member(member).slotIndex(assignedSlot).status(BattleParticipantStatus.WAITING).build();
		battleParticipantRepository.save(entity);
		messagingTemplate.convertAndSend("/topic/room/" + roomId, Map.of("type", "join", "memberId", userId, "username", member != null ? member.getUsername() : "Unknown", "slotIndex", assignedSlot));
	}

	@Transactional
	public void updateParticipantStatus(Long roomId, Long userId, BattleParticipantStatus status) {
		battleParticipantRepository.findByRoomIdAndMemberId(roomId, userId).ifPresent(p -> {
			p.setStatus(status);
			battleParticipantRepository.save(p);
			messagingTemplate.convertAndSend("/topic/room/" + roomId, Map.of("type", "status", "message", status == BattleParticipantStatus.READY ? "READY" : "UNREADY", "memberId", userId, "slotIndex", p.getSlotIndex()));
		});
	}

	@Transactional
	public void broadcastGameStart(Long roomId, Long seed) {
		battleRoomRepository.findById(roomId).ifPresent(room -> {
			messagingTemplate.convertAndSend("/topic/room/" + roomId, Map.of(
				"type", "status", 
				"message", "GAME_START", 
				"seed", seed,
				"mode", room.getBattleMode().name()
			));
		});
	}

	@Transactional
	public void leaveRoom(Long roomId, Long userId) {
		List<BattleParticipantEntity> participants = battleParticipantRepository.findByRoomId(roomId);
		BattleParticipantEntity leavingUser = participants.stream().filter(p -> p.getMemberId().equals(userId)).findFirst().orElse(null);
		int slotIndex = (leavingUser != null) ? leavingUser.getSlotIndex() : -1;
		battleParticipantRepository.deleteByRoomIdAndMemberId(roomId, userId);
		List<BattleParticipantEntity> others = battleParticipantRepository.findByRoomId(roomId);
		if (others.isEmpty()) {
			battleRoomRepository.findById(roomId).ifPresent(room -> { room.setStatus(BattleRoomStatus.FINISHED); room.setActualStartedAt(null); battleRoomRepository.save(room); });
		} else {
			messagingTemplate.convertAndSend("/topic/room/" + roomId, Map.of("type", "leave", "memberId", userId, "slotIndex", slotIndex));
			for (BattleParticipantEntity p : others) sseService.send(p.getMemberId(), "leave", Map.of("UserId", userId, "SlotIndex", slotIndex));
		}
	}

	public List<Map<String, Object>> getAllParticipantPositions(Long roomId) {
		return battleParticipantRepository.findByRoomId(roomId).stream().map(p -> {
			BattleRogueProgressEntity prog = battleRogueProgressRepository.findByRoomIdAndMemberId(roomId, p.getMemberId())
					.orElseGet(() -> BattleRogueProgressEntity.builder().roomId(roomId).memberId(p.getMemberId()).currentNodeId("0-0").cumulativeScore(0).isFinished(false).visitedNodesJson("[]").visitedPathsJson("[]").build());
			return Map.<String, Object>of(
				"memberId", p.getMemberId(), "nodeId", prog.getCurrentNodeId(), "isCleared", prog.isFinished(),
				"slotIndex", p.getSlotIndex(), "username", p.getMember() != null ? p.getMember().getUsername() : "Unknown",
				"score", prog.getCumulativeScore(), "status", p.getStatus().name(),
				"visitedNodes", prog.getVisitedNodesJson() != null ? prog.getVisitedNodesJson() : "[]",
				"visitedPaths", prog.getVisitedPathsJson() != null ? prog.getVisitedPathsJson() : "[]"
			);
		}).toList();
	}

	@Transactional
	public void updateRogueProgress(Long roomId, Long userId, String nodeId, boolean isCleared, int scoreGain, String visitedNodesJson, String visitedPathsJson) {
		BattleRogueProgressEntity prog = battleRogueProgressRepository.findByRoomIdAndMemberId(roomId, userId)
				.orElse(BattleRogueProgressEntity.builder().roomId(roomId).memberId(userId).cumulativeScore(0).build());
		if (nodeId != null) prog.setCurrentNodeId(nodeId);
		prog.setFinished(isCleared);
		if (isCleared) prog.setCumulativeScore(prog.getCumulativeScore() + scoreGain);
		if (visitedNodesJson != null) prog.setVisitedNodesJson(visitedNodesJson);
		if (visitedPathsJson != null) prog.setVisitedPathsJson(visitedPathsJson);
		battleRogueProgressRepository.save(prog);

		// 보스 노드(10층) 클리어 시 전체 배틀 상태를 FINISHED로 변경
		if (isCleared && nodeId != null && nodeId.startsWith("10-")) {
			updateParticipantStatus(roomId, userId, com.passfail.enums.BattleParticipantStatus.FINISHED, prog.getCumulativeScore());
		}

		messagingTemplate.convertAndSend("/topic/room/" + roomId, Map.of("type", "position", "memberId", userId, "nodeId", nodeId != null ? nodeId : "", "isCleared", isCleared, "score", prog.getCumulativeScore(), "slotIndex", battleParticipantRepository.findByRoomIdAndMemberId(roomId, userId).map(BattleParticipantEntity::getSlotIndex).orElse(0)));
	}

	@Transactional
	public void checkAndCleanupRoom(Long roomId) {
		scheduler.schedule(() -> performCleanupIfStillEmpty(roomId), 60, TimeUnit.SECONDS);
	}

	@Transactional
	public void performCleanupIfStillEmpty(Long roomId) {
		battleRoomRepository.findById(roomId).ifPresent(room -> {
			if (room.getStatus() == BattleRoomStatus.IN_PROGRESS) {
				List<BattleParticipantEntity> participants = battleParticipantRepository.findByRoomId(roomId);
				if (participants.stream().allMatch(p -> p.getStatus() == BattleParticipantStatus.DISCONNECTED) && !participants.isEmpty()) {
					battleParticipantRepository.deleteByRoomId(roomId);
					battleRogueProgressRepository.deleteByRoomId(roomId);
					room.setStatus(BattleRoomStatus.FINISHED); room.setActualStartedAt(null); battleRoomRepository.save(room);
				}
			}
		});
	}
}
