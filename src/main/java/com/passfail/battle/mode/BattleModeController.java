package com.passfail.battle.mode;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.passfail.battle.room.BattleRoomRepository;
import com.passfail.battle.util.Mulberry32;
import com.passfail.entity.BattleRoomEntity;
import com.passfail.entity.MemberEntity;
import com.passfail.member.repository.MemberRepository;
import java.time.ZoneId;
import java.time.ZonedDateTime;


@Controller
@RequestMapping("/battle/mode")
public class BattleModeController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BattleRoomRepository battleRoomRepository;

    @Autowired
    private com.passfail.problem.service.ProblemService problemService;

	// 맵 페이지
	@GetMapping("/rogueMap")
	public String list(@RequestParam(value = "seed", required = false) Long seed, 
					   @RequestParam(value = "roomId", required = false) Long roomId,
					   Principal principal, Model model) {
		System.out.println("rogueMap - roomId: " + roomId);
		if (seed == null) {
			seed = (long) (Math.random() * 1000000);
		}

		generateServerMap(seed);

		model.addAttribute("mapSeed", seed);
		model.addAttribute("roomId", roomId);

		if (roomId != null) {
			battleRoomRepository.findById(roomId).ifPresent(room -> {
				if (room.getActualStartedAt() != null) {
					// 클라이언트 JS에서 사용하기 쉽게 Epoch Milli로 변환하여 전달
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
        
        if (room.getProblemId() == null) {
            return "redirect:/battle/room/join/" + roomId;
        }

        com.passfail.problem.dto.ProblemResponse problem = problemService.getProblemResponse(room.getProblemId());
        MemberEntity member = memberRepository.findByUsername(principal.getName()).orElseThrow();

        model.addAttribute("room", room);
        model.addAttribute("problem", problem);
        model.addAttribute("roomId", roomId);
        model.addAttribute("seed", seed);
        model.addAttribute("currentUserId", member.getMemberId());
        model.addAttribute("battleMode", room.getBattleMode().name());
        
        // 에디터 초기 코드 설정 (모드별 분기)
        String defaultCode = "";
        if (room.getBattleMode() == com.passfail.enums.BattleMode.LOGIC_MAZE) {
            defaultCode = "import java.util.*;\n\n" +
                          "public class Solution {\n" +
                          "    public static void main(String[] args) {\n" +
                          "        Robot robot = new Robot();\n" +
                          "        \n" +
                          "        // [에너지 최적화 미션]\n" +
                          "        // - 이동/회전은 코스트가 높고, 감지는 낮습니다.\n" +
                          "        // - 효율적인 알고리즘으로 최소 비용 탈출을 기록하세요.\n" +
                          "        \n" +
                          "        while(!robot.getCurrentTileType().equals(\"EXIT\")) {\n" +
                          "            if(!robot.isWallAhead()) {\n" +
                          "                robot.moveForward();\n" +
                          "            } else {\n" +
                          "                robot.turnRight();\n" +
                          "            }\n" +
                          "        }\n" +
                          "    }\n" +
                          "}\n\n" +
                          "// --- 플랫폼 제공 API (수정 금지) ---\n" +
                          "class Robot {\n" +
                          "    private int x, y, dir; // 0:E, 1:S, 2:W, 3:N\n" +
                          "    private Map<String, String> grid = new HashMap<>();\n" +
                          "    private int width, height, cost = 0;\n" +
                          "    private Scanner sc = new Scanner(System.in);\n\n" +
                          "    public Robot() {\n" +
                          "        if (sc.hasNextInt()) {\n" +
                          "            this.width = sc.nextInt(); this.height = sc.nextInt();\n" +
                          "            while (sc.hasNext()) {\n" +
                          "                String type = sc.next();\n" +
                          "                if (type.equals(\"START\")) { this.x = sc.nextInt(); this.y = sc.nextInt(); grid.put(x+\",\"+y, \"START\"); }\n" +
                          "                else if (type.equals(\"EXIT\")) { int ex = sc.nextInt(); int ey = sc.nextInt(); grid.put(ex+\",\"+ey, \"EXIT\"); }\n" +
                          "                else if (type.equals(\"WALL\")) { grid.put(sc.nextInt() + \",\" + sc.nextInt(), \"WALL\"); }\n" +
                          "                else if (type.equals(\"SWITCH\")) { sc.next(); grid.put(sc.nextInt() + \",\" + sc.nextInt(), \"SWITCH\"); }\n" +
                          "                else if (type.equals(\"DOOR\")) { sc.next(); grid.put(sc.nextInt() + \",\" + sc.nextInt(), \"DOOR\"); sc.next(); }\n" +
                          "                else if (type.equals(\"ITEM\")) { int val = sc.nextInt(); grid.put(sc.nextInt()+\",\"+sc.nextInt(), \"ITEM:\"+val); }\n" +
                          "                else if (type.equals(\"END\")) break;\n" +
                          "            }\n" +
                          "        }\n" +
                          "        this.dir = 0;\n" +
                          "    }\n\n" +
                          "    private void waitAck() { if(sc.hasNext()) sc.next(); }\n" +
                          "    private void addCost(int c) { cost += c; System.out.println(\"CMD:COST:\" + cost); System.out.flush(); }\n\n" +
                          "    public void moveForward() { addCost(5); System.out.println(\"CMD:MOVE\"); System.out.flush(); waitAck(); if(!isWallAhead()){ if(dir==0)x++; else if(dir==1)y++; else if(dir==2)x--; else y--; } }\n" +
                          "    public void turnLeft() { addCost(5); System.out.println(\"CMD:LEFT\"); System.out.flush(); waitAck(); dir=(dir+3)%4; }\n" +
                          "    public void turnRight() { addCost(5); System.out.println(\"CMD:RIGHT\"); System.out.flush(); waitAck(); dir=(dir+1)%4; }\n" +
                          "    public void useSwitch() { addCost(5); System.out.println(\"CMD:USE\"); System.out.flush(); waitAck(); }\n" +
                          "    public void pickUp() { addCost(5); System.out.println(\"CMD:PICKUP\"); System.out.flush(); waitAck(); }\n" +
                          "    public void say(String m) { System.out.println(\"CMD:SAY:\"+m); System.out.flush(); waitAck(); }\n\n" +
                          "    public int readValue() { addCost(1); String s = grid.getOrDefault(x+\",\"+y, \"\"); return s.startsWith(\"ITEM:\") ? Integer.parseInt(s.split(\":\")[1]) : -1; }\n" +
                          "    public String getCurrentTileType() { addCost(1); String s = grid.getOrDefault(x+\",\"+y, \"EMPTY\"); return s.startsWith(\"ITEM\") ? \"ITEM\" : s; }\n" +
                          "    public boolean isWallAhead() {\n" +
                          "        addCost(1);\n" +
                          "        int nx=x+(dir==0?1:dir==2?-1:0), ny=y+(dir==1?1:dir==3?-1:0);\n" +
                          "        String t = grid.getOrDefault(nx+\",\"+ny, \"EMPTY\");\n" +
                          "        return nx<0 || nx>=width || ny<0 || ny>=height || t.equals(\"WALL\") || t.equals(\"DOOR\");\n" +
                          "    }\n" +
                          "}";
        } else {
            defaultCode = "import java.util.*;\n\npublic class Solution {\n" +
                          "    public static void main(String[] args) {\n" +
                          "        Scanner sc = new Scanner(System.in);\n" +
                          "        // 코드를 작성하세요\n" +
                          "    }\n" +
                          "}";
        }
        
        model.addAttribute("defaultCode", defaultCode);

        // 모드별 전용 페이지로 분기 (Thymeleaf 레이아웃 상속)
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

	    // 1. 노드 배치 (0층~9층)
	    for (int f = 0; f < 10; f++) {
	        List<MapNode> floor = new ArrayList<>();
	        int nodeCount = (f == 0 || f == 9) ? 1 : (int) Math.floor(targetRandom.getRandom() * 3) + 2;

	        for (int n = 0; n < nodeCount; n++) {
	            String type = (f == 9) ? "BOSS" : (targetRandom.getRandom() > 0.15 ? "Puzzle" : "Event");
	            floor.add(new MapNode(f + "-" + n, type));
	        }
	        mapData.add(floor);
	    }

	    // 2. 노드 간 연결 생성 (JS와 동일한 순서)
	    for (int f = 0; f < mapData.size() - 1; f++) {                                                                                                                                                                                                                                                                                                                             
	        List<MapNode> currentFloor = mapData.get(f);
	        List<MapNode> nextFloor = mapData.get(f + 1);

	        // (1) Forward 연결: 현재 층 -> 다음 층
	        for (int i = 0; i < currentFloor.size(); i++) {
	            MapNode node = currentFloor.get(i);
	            
	            // 인덱스 비율로 타겟 결정
	            int targetIdx = (int) Math.floor(((double) i / currentFloor.size()) * nextFloor.size());
	            node.connectedTo.add(nextFloor.get(targetIdx).id);

	            // 확률적으로 하나 더 연결 (길 갈라짐)
	            if (targetIdx + 1 < nextFloor.size() && targetRandom.getRandom() > 0.5) {
	                node.connectedTo.add(nextFloor.get(targetIdx + 1).id);
	            }
	        }

	        // (2) Backward 보정: 다음 층에서 '고립된' 노드 연결
	        for (int ni = 0; ni < nextFloor.size(); ni++) {
	            MapNode nextNode = nextFloor.get(ni);
	            
	            // 부모가 있는지 확인
	            boolean hasParent = false;
	            for (MapNode currNode : currentFloor) {
	                if (currNode.connectedTo.contains(nextNode.id)) {
	                    hasParent = true;
	                    break;
	                }
	            }

	            if (!hasParent) {
	                // 부모가 없다면 아래층에서 가장 가까운 노드에 강제로 연결
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