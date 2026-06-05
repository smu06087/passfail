package com.passfail.codingtest.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 사용자 코드를 Docker 컨테이너 내에서 실행하기 위한 유틸리티 클래스
 */
@Component
@Slf4j
public class DockerRunner {

    @Getter
    @AllArgsConstructor
    public static class DockerResult {
        private final int exitCode;
        private final String output;
        private final boolean timeout;
    }

    public DockerResult run(List<String> cmd, int timeoutSeconds) throws Exception {
        log.info("[DockerRunner] Executing command: {}", String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();

        Thread streamReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (Exception e) {
                log.error("[DockerRunner] Error reading stream", e);
            }
        });
        streamReader.setName("docker-stdout-reader");
        streamReader.start();

        boolean finished = process.waitFor(timeoutSeconds + 2, TimeUnit.SECONDS);

        if (!finished) {
            log.warn("[DockerRunner] Command timed out after {} seconds", timeoutSeconds);
            process.destroyForcibly();
            streamReader.interrupt();
            return new DockerResult(-1, output.toString() + "\n[ERROR] Execution Timeout", true);
        }

        streamReader.join(1000);
        String resultOutput = output.toString().trim();
        log.info("[DockerRunner] Command finished with exit code: {}", process.exitValue());
        log.debug("[DockerRunner] Full output: \n{}", resultOutput);

        return new DockerResult(process.exitValue(), resultOutput, false);
    }
}

