package com.passfail.battle.mode;

import com.passfail.codingtest.util.DefaultCodeProvider;
import com.passfail.codingtest.util.JudgeEnvironmentProvider;
import com.passfail.enums.ProgrammingLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class InteractiveMazeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final DefaultCodeProvider codeProvider;
    private final JudgeEnvironmentProvider envProvider;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();
    private final Map<String, BufferedWriter> processInputs = new ConcurrentHashMap<>();
    private final Map<String, List<String>> commandBuffers = new ConcurrentHashMap<>();

    public InteractiveMazeService(SimpMessagingTemplate messagingTemplate, 
                                  DefaultCodeProvider codeProvider,
                                  JudgeEnvironmentProvider envProvider) {
        this.messagingTemplate = messagingTemplate;
        this.codeProvider = codeProvider;
        this.envProvider = envProvider;
    }

    public void startExecution(Long roomId, Long memberId, String code, String mapData, ProgrammingLanguage language) {
        String key = roomId + ":" + memberId;
        stopExecution(roomId, memberId);
        commandBuffers.put(key, new ArrayList<>());

        executorService.submit(() -> {
            Path tempDir = null;
            try {
                tempDir = Files.createTempDirectory("maze_run_" + memberId);
                
                // 0. 미로 데이터 파싱 및 로봇 상태 초기화
                MazeState state = new MazeState(mapData);
                
                // 1. 코드 주입
                String fullCode = code + codeProvider.getLogicMazeImplementation(language);
                
                // 2. 파일 저장
                String fileName = switch (language) {
                    case JAVA -> "Solution.java";
                    case PYTHON -> "main.py";
                    case CPP -> "main.cpp";
                    default -> "code.txt";
                };
                Files.writeString(tempDir.resolve(fileName), fullCode, StandardCharsets.UTF_8);

                // 3. 환경 준비 및 컴파일
                ProcessBuilder pb = envProvider.isUsingDocker() ? createDockerProcessBuilder(language, tempDir) : createLocalProcessBuilder(language, tempDir);

                if (language == ProgrammingLanguage.JAVA || language == ProgrammingLanguage.CPP) {
                    Process compileProcess = envProvider.isUsingDocker() ? createDockerCompileProcess(language, tempDir) : createCompileProcess(language, tempDir);
                    if (compileProcess != null) {

                        StringBuilder compileErrorLog = new StringBuilder();
                        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                            String errLine;
                            while ((errLine = errorReader.readLine()) != null) {
                                compileErrorLog.append(errLine).append("\n");
                            }
                        }

                        boolean finished = compileProcess.waitFor(20, TimeUnit.SECONDS);

                        if (!finished || compileProcess.exitValue() != 0) {
                            log.error("컴파일 실패 로그:\n{}", compileErrorLog.toString());
                                                        
                            String ShortError = compileErrorLog.length() > 100 ? compileErrorLog.substring(0, 100) + "..." : compileErrorLog.toString();
                            sendToClient(roomId, memberId, List.of("CMD:SAY:컴파일 실패!\n" + ShortError));
                            return;
                        }
                    }
                }

                // 4. 실행
                Process runProcess = pb.redirectErrorStream(true).start();
                activeProcesses.put(key, runProcess);

                int staticCost = calculateStaticCost(code);
                sendToClient(roomId, memberId, List.of("CMD:SAY:로직 실행 시작...", "CMD:STATIC_COST:" + staticCost));

                try (
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream()));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))
                ) {
                    processInputs.put(key, writer);
                    
                    // 초기 기동: 미로 데이터는 사실 서버가 이미 알고 있으므로 무시하거나 전달
                    String line;
                    while (runProcess.isAlive() && (line = reader.readLine()) != null) {
                        String cmd = line.trim();
                        if (cmd.startsWith("CMD:")) {
                            // 서버측 상태 업데이트 및 질문 응답
                            handleRobotCommand(cmd, state, writer, roomId, memberId);
                        } else {
                            // 일반 출력 (디버깅용 로그로 전환)
                            if (!cmd.isEmpty()) sendToClient(roomId, memberId, List.of("CMD:SAY:" + cmd));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Execution error", e);
                sendToClient(roomId, memberId, List.of("CMD:SAY:시스템 오류: " + e.getMessage()));
            } finally {
                stopExecution(roomId, memberId);
                if (tempDir != null) deleteDirectory(tempDir.toFile());
            }
        });
    }

    private void handleRobotCommand(String cmd, MazeState state, BufferedWriter writer, Long roomId, Long memberId) throws IOException {
        String type = cmd.replace("CMD:", "");
        List<String> toSend = new ArrayList<>();
        toSend.add(cmd); // 클라이언트에게도 전달하여 시각화

        if (type.equals("WALL")) {
            boolean isWall = state.isWallAhead();
            writer.write(isWall + "\n");
            writer.flush();
        } else if (type.equals("TILE")) {
            String tile = state.getCurrentTileType();
            writer.write(tile + "\n");
            writer.flush();
        } else if (type.equals("MOVE")) {
            state.move();
        } else if (type.equals("LEFT")) {
            state.turnLeft();
        } else if (type.equals("RIGHT")) {
            state.turnRight();
        } else if (type.equals("USE")) {
            state.useSwitch();
        }

        // 진행 상황 전송
        sendToClient(roomId, memberId, toSend);
    }

    // --- Helper Methods ---

    private ProcessBuilder createLocalProcessBuilder(ProgrammingLanguage lang, Path dir) {
        ProcessBuilder pb = switch (lang) {
            case JAVA -> new ProcessBuilder("java", "-cp", ".", "Solution");
            case PYTHON -> new ProcessBuilder("py", "main.py");
            case CPP -> new ProcessBuilder("./main.exe");
            default -> throw new RuntimeException("Unsupported language");
        };
        pb.directory(dir.toFile());
        return pb;
    }

    private Process createCompileProcess(ProgrammingLanguage lang, Path dir) throws IOException {
        ProcessBuilder pb = switch (lang) {
            case JAVA -> new ProcessBuilder("javac", "-encoding", "UTF-8", "Solution.java");
            case CPP -> new ProcessBuilder("g++", "-O2", "main.cpp", "-o", "main.exe");
            default -> null;
        };
        if (pb == null) return null;
        pb.directory(dir.toFile());
        return pb.start();
    }

    private ProcessBuilder createDockerProcessBuilder(ProgrammingLanguage lang, Path dir) {
        String runCmd = switch (lang) {
            case JAVA -> "java -cp . Solution";
            case PYTHON -> "python3 main.py";
            case CPP -> "./main";
            default -> "exit 1";
        };
        List<String> cmd = List.of("docker", "run", "-i", "--rm", "-v", dir.toAbsolutePath() + ":/workspace", "--workdir=/workspace", "--network=none", "--memory", "256m", "--cpus", "0.5", "judge-sandbox", "/bin/bash", "-c", runCmd);
        return new ProcessBuilder(cmd);
    }

    private ProcessBuilder createDockerCompileProcess(ProgrammingLanguage lang, Path dir) throws IOException {
        String compileCmd = (lang == ProgrammingLanguage.JAVA) ? "javac -encoding UTF-8 Solution.java" : "g++ -O2 main.cpp -o main";
        List<String> cmd = List.of("docker", "run", "--rm", "-v", dir.toAbsolutePath() + ":/workspace", "--workdir=/workspace", "judge-sandbox", "/bin/bash", "-c", compileCmd);
        return new ProcessBuilder(cmd);
    }

    public void handleControl(Long roomId, Long memberId, String action) {
        String key = roomId + ":" + memberId;
        if ("STOP".equals(action)) {
            stopExecution(roomId, memberId);
        } else if ("MORE".equals(action)) {
            BufferedWriter writer = processInputs.get(key);
            if (writer != null) {
                try {
                    writer.write("OK\n");
                    writer.flush();
                } catch (IOException e) { log.error("Sync error", e); }
            }
        }
    }

    private void stopExecution(Long roomId, Long memberId) {
        String key = roomId + ":" + memberId;
        processInputs.remove(key);
        commandBuffers.remove(key);
        Process p = activeProcesses.remove(key);
        if (p != null) p.destroyForcibly();
    }

    private void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        if (contents != null) { for (File f : contents) deleteDirectory(f); }
        file.delete();
    }

    private int calculateStaticCost(String code) {
        int cost = 0;
        String[] actions = {"moveForward", "turnLeft", "turnRight", "useSwitch", "pickUp", "move_forward", "turn_left", "turn_right", "use_switch", "pick_up"};
        for (String a : actions) cost += countOccurrences(code, "robot." + a) * 2;
        return cost;
    }

    private int countOccurrences(String text, String target) {
        return (text.length() - text.replace(target, "").length()) / target.length();
    }

    private void sendToClient(Long roomId, Long memberId, List<String> data) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId, Map.of("type", "progress", "memberId", memberId, "data", String.join("|", data)));
    }

    // --- Inner Class: Maze State Tracker ---
    private static class MazeState {
        int width, height, rx, ry, dir = 0; // 0:E, 1:S, 2:W, 3:N
        Set<String> walls = new HashSet<>();
        Map<String, String> items = new HashMap<>();
        Map<String, String> doors = new HashMap<>(); // "x,y" -> switchId
        Set<String> activeSwitches = new HashSet<>();
        String exitPos;

        MazeState(String mapData) {
            String[] lines = mapData.split("\n");
            for (String line : lines) {
                String[] p = line.split(" ");
                if (p.length < 2) continue;
                switch (p[0]) {
                    case "START" -> { rx = Integer.parseInt(p[1]); ry = Integer.parseInt(p[2]); }
                    case "EXIT" -> exitPos = p[1] + "," + p[2];
                    case "WALL" -> walls.add(p[1] + "," + p[2]);
                    case "SWITCH" -> {} // 스위치는 위치만 파악 (나중에 USE 시 체크)
                    case "DOOR" -> doors.put(p[2] + "," + p[3], p[4]);
                    case "ITEM" -> items.put(p[2] + "," + p[3], p[1]);
                }
            }
        }

        boolean isWallAhead() {
            int nx = rx + (dir == 0 ? 1 : dir == 2 ? -1 : 0);
            int ny = ry + (dir == 1 ? 1 : dir == 3 ? -1 : 0);
            String pos = nx + "," + ny;
            if (walls.contains(pos)) return true;
            if (doors.containsKey(pos)) {
                String swId = doors.get(pos);
                return !activeSwitches.contains(swId);
            }
            return false;
        }

        String getCurrentTileType() {
            String pos = rx + "," + ry;
            if (pos.equals(exitPos)) return "EXIT";
            return "PATH";
        }

        void move() {
            if (!isWallAhead()) {
                rx += (dir == 0 ? 1 : dir == 2 ? -1 : 0);
                ry += (dir == 1 ? 1 : dir == 3 ? -1 : 0);
            }
        }
        void turnLeft() { dir = (dir + 3) % 4; }
        void turnRight() { dir = (dir + 1) % 4; }
        void useSwitch() { /* 필요 시 스위치 활성화 로직 추가 */ }
    }
}
