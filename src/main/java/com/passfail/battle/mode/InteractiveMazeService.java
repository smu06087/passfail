package com.passfail.battle.mode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class InteractiveMazeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();
    private final Map<String, BufferedWriter> processInputs = new ConcurrentHashMap<>();
    private final Map<String, List<String>> commandBuffers = new ConcurrentHashMap<>();

    public InteractiveMazeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void startExecution(Long roomId, Long memberId, String code, String mapData) {
        String key = roomId + ":" + memberId;
        stopExecution(roomId, memberId);
        commandBuffers.put(key, new ArrayList<>());

        executorService.submit(() -> {
            Path tempDir = null;
            try {
                tempDir = Files.createTempDirectory("maze_run_" + memberId);
                File sourceFile = new File(tempDir.toFile(), "Solution.java");
                Files.writeString(sourceFile.toPath(), code);

                Process compileProcess = new ProcessBuilder("javac", sourceFile.getAbsolutePath()).start();
                if (!compileProcess.waitFor(10, TimeUnit.SECONDS) || compileProcess.exitValue() != 0) {
                    sendToClient(roomId, memberId, List.of("CMD:SAY:컴파일 실패"));
                    return;
                }

                Process runProcess = new ProcessBuilder("java", "-cp", tempDir.toString(), "Solution")
                        .redirectErrorStream(true).start();
                activeProcesses.put(key, runProcess);

                int staticCost = calculateStaticCost(code);
                sendToClient(roomId, memberId, List.of("CMD:STATIC_COST:" + staticCost));

                try (
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream()));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))
                ) {
                    processInputs.put(key, writer);
                    writer.write(mapData);
                    writer.write("END\n");
                    writer.flush();

                    // 초기 기동: 버퍼를 채우기 위해 OK를 자동으로 보냄
                    for(int i=0; i<100; i++) { writer.write("OK\n"); }
                    writer.flush();

                    String line;
                    while (runProcess.isAlive() && (line = reader.readLine()) != null) {
                        if (line.startsWith("CMD:")) {
                            List<String> buffer = commandBuffers.get(key);
                            if (buffer != null) {
                                buffer.add(line);
                                // 버퍼가 50개 쌓이면 클라이언트로 발송
                                if (buffer.size() >= 50) {
                                    sendToClient(roomId, memberId, new ArrayList<>(buffer));
                                    buffer.clear();
                                }
                                
                                // 만약 전체 쌓인 명령어가 너무 많으면(1000개), Java에게 더 이상 OK를 주지 않음으로써 대기시킴
                                // 이 로직은 handleControl에서 "MORE" 요청 시마다 writer.write("OK\n")를 더 해주는 방식으로 구현
                            }
                        }
                    }
                    // 남은 버퍼 발송
                    List<String> lastBuffer = commandBuffers.get(key);
                    if (lastBuffer != null && !lastBuffer.isEmpty()) {
                        sendToClient(roomId, memberId, lastBuffer);
                    }
                }
            } catch (Exception e) {
                log.error("Execution error", e);
            } finally {
                stopExecution(roomId, memberId);
                if (tempDir != null) deleteDirectory(tempDir.toFile());
            }
        });
    }

    public void handleControl(Long roomId, Long memberId, String action) {
        String key = roomId + ":" + memberId;
        if ("STOP".equals(action)) {
            stopExecution(roomId, memberId);
        } else if ("MORE".equals(action)) {
            // 클라이언트가 데이터를 더 요청하면 Java 프로세스에게 다음 단계 진행 신호(OK)를 뭉치로 보냄
            BufferedWriter writer = processInputs.get(key);
            if (writer != null) {
                try {
                    for(int i=0; i<50; i++) { writer.write("OK\n"); }
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
        String[] actions = {"moveForward", "turnLeft", "turnRight", "useSwitch", "pickUp"};
        for (String a : actions) cost += countOccurrences(code, "robot." + a) * 2;
        String[] senses = {"isWallAhead", "readValue", "getCurrentTileType"};
        for (String s : senses) cost += countOccurrences(code, "robot." + s) * 1;
        return cost;
    }

    private int countOccurrences(String text, String target) {
        return (text.length() - text.replace(target, "").length()) / target.length();
    }

    private void sendToClient(Long roomId, Long memberId, List<String> data) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId, Map.of(
            "type", "progress",
            "memberId", memberId,
            "data", String.join("|", data) // 명령어들을 구분자 | 로 합쳐서 전송
        ));
    }
}
