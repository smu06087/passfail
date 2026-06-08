package com.passfail.battle.mode;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.passfail.battle.room.BattleRoomRepository;
import com.passfail.battle.util.Mulberry32;
import com.passfail.codingtest.util.DefaultCodeProvider;
import com.passfail.entity.BattleRoomEntity;
import com.passfail.entity.MemberEntity;
import com.passfail.enums.ProgrammingLanguage;
import com.passfail.member.repository.MemberRepository;
import java.time.ZoneId;

@Controller
@RequestMapping("/battle/mode")
public class BattleModeController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BattleRoomRepository battleRoomRepository;

    @Autowired
    private com.passfail.problem.service.ProblemService problemService;

    @Autowired
    private DefaultCodeProvider codeProvider;

    @Autowired
    private com.passfail.codingtest.util.JudgeEnvironmentProvider envProvider;

    @ModelAttribute("isLinux")
    public boolean isLinux() {
        return envProvider.isUsingDocker();
    }

	// 맵 페이지
	@GetMapping("/rogueMap")
	public String list(@RequestParam(value = "seed", required = false) Long seed, 
					   @RequestParam(value = "roomId", required = false) Long roomId,
					   Principal principal, Model model) {
		if (seed == null) {
			seed = (long) (Math.random() * 1000000);
		}

		generateServerMap(seed);

		model.addAttribute("mapSeed", seed);
		model.addAttribute("roomId", roomId);

		if (roomId != null) {
			battleRoomRepository.findById(roomId).ifPresent(room -> {
				if (room.getActualStartedAt() != null) {
					long startTimeMillis = room.getActualStartedAt()
							.atZone(ZoneId.systemDefault())
							.toInstant()
							.toEpochMilli();
					model.addAttribute("battleStartTime", startTimeMillis);
				}
			});
		}

		if (principal != null) {
			MemberEntity member = memberRepository.findByUsername(principal.getName()).orElse(null);
			if (member != null) {
				model.addAttribute("currentUserId", member.getMemberId());
			}
		}

		return "battle/mode/rogueMap";
	}

    // 통합 에디터 페이지 (QUICK, GOLF, LOGIC_MAZE 등)
    @GetMapping("/editor")
    public String battleEditor(@RequestParam("roomId") Long roomId,
                               @RequestParam(value = "seed", required = false) Long seed,
                               Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        BattleRoomEntity room = battleRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid room ID"));
        
        // LOGIC_MAZE가 아닐 때만 problemId 체크
        if (room.getProblemId() == null && room.getBattleMode() != com.passfail.enums.BattleMode.LOGIC_MAZE) {
            return "redirect:/battle/room/join/" + roomId;
        }

        com.passfail.problem.dto.ProblemResponse problem;
        if (room.getProblemId() != null) {
            problem = problemService.getProblemResponse(room.getProblemId());
        } else {
            // LOGIC_MAZE 전용 기본 설명 객체 생성
            problem = com.passfail.problem.dto.ProblemResponse.builder()
                    .problemId(0L)
                    .title("로직 메이즈")
                    .description("로직 메이즈 모드입니다. 로봇을 조종하여 출구까지 이동시키세요!")
                    .testCases(new ArrayList<>())
                    .timeLimitMs(10000)
                    .memoryLimitMb(256)
                    .build();
        }

        MemberEntity member = memberRepository.findByUsername(principal.getName()).orElseThrow();

        model.addAttribute("room", room);
        model.addAttribute("problem", problem);
        model.addAttribute("roomId", roomId);
        model.addAttribute("seed", seed);
        model.addAttribute("currentUserId", member.getMemberId());
        model.addAttribute("battleMode", room.getBattleMode().name());
        
        // OnDocker: DefaultCodeProvider를 사용하여 모드 및 언어별 템플릿 제공
        String defaultCode;
        if (room.getBattleMode() == com.passfail.enums.BattleMode.LOGIC_MAZE) {
            defaultCode = codeProvider.getLogicMazeEditorCode(ProgrammingLanguage.JAVA);
            model.addAttribute("logicMazeApi", codeProvider.getLogicMazeApiDocs());
        } else {
            defaultCode = codeProvider.getDefaultCode(ProgrammingLanguage.JAVA);
        }
        
        model.addAttribute("defaultCode", defaultCode);

        // 모드별 전용 페이지로 분기
        String modePath = room.getBattleMode().name().toLowerCase();
        if (modePath.contains("_")) {
            String[] parts = modePath.split("_");
            StringBuilder sb = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            }
            modePath = sb.toString();
        }

        return "battle/mode/" + modePath;
    }

	public List<List<MapNode>> generateServerMap(long seed) {
	    Mulberry32 targetRandom = Mulberry32.getInstance(seed);
	    List<List<MapNode>> mapData = new ArrayList<>();

	    for (int f = 0; f < 10; f++) {
	        List<MapNode> floor = new ArrayList<>();
	        int nodeCount = (f == 0 || f == 9) ? 1 : (int) Math.floor(targetRandom.getRandom() * 3) + 2;

	        for (int n = 0; n < nodeCount; n++) {
	            String type = (f == 9) ? "BOSS" : (targetRandom.getRandom() > 0.15 ? "Puzzle" : "Event");
	            floor.add(new MapNode(f + "-" + n, type));
	        }
	        mapData.add(floor);
	    }

	    for (int f = 0; f < mapData.size() - 1; f++) {                                                                                                                                                                                                                                                                                                                             
	        List<MapNode> currentFloor = mapData.get(f);
	        List<MapNode> nextFloor = mapData.get(f + 1);

	        for (int i = 0; i < currentFloor.size(); i++) {
	            MapNode node = currentFloor.get(i);
	            int targetIdx = (int) Math.floor(((double) i / currentFloor.size()) * nextFloor.size());
	            node.connectedTo.add(nextFloor.get(targetIdx).id);
	            if (targetIdx + 1 < nextFloor.size() && targetRandom.getRandom() > 0.5) {
	                node.connectedTo.add(nextFloor.get(targetIdx + 1).id);
	            }
	        }

	        for (int ni = 0; ni < nextFloor.size(); ni++) {
	            MapNode nextNode = nextFloor.get(ni);
	            boolean hasParent = false;
	            for (MapNode currNode : currentFloor) {
	                if (currNode.connectedTo.contains(nextNode.id)) {
	                    hasParent = true;
	                    break;
	                }
	            }
	            if (!hasParent) {
	                int parentIdx = (int) Math.floor(((double) ni / nextFloor.size()) * currentFloor.size());
	                currentFloor.get(parentIdx).connectedTo.add(nextNode.id);
	            }
	        }
	    }
	    return mapData;
	}
}

class MapNode {
    public String id;
    public String type;
    public List<String> connectedTo = new ArrayList<>();

    public MapNode(String id, String type) {
        this.id = id;
        this.type = type;
    }
}
