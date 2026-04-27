package com.passfail.codingtest.service;

import com.passfail.codingtest.dto.ExecutionResult;
import com.passfail.codingtest.dto.CustomTestCaseRequest;
import com.passfail.problem.dto.ProblemResponse;
import com.passfail.problem.dto.TestCaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CodeExecutionService {

    public List<ExecutionResult> executeJava(ProblemResponse problem, String code, List<CustomTestCaseRequest> customTestCases) {
        List<ExecutionResult> results = new ArrayList<>();
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("codingtest_");
            Path sourcePath = tempDir.resolve("Solution.java");
            Files.writeString(sourcePath, code);

            ProcessBuilder compileBuilder = new ProcessBuilder("javac", "Solution.java");
            compileBuilder.directory(tempDir.toFile());
            Process compileProcess = compileBuilder.start();
            boolean compiled = compileProcess.waitFor(10, TimeUnit.SECONDS);

            if (!compiled || compileProcess.exitValue() != 0) {
                String error = new String(compileProcess.getErrorStream().readAllBytes());
                results.add(ExecutionResult.builder()
                        .success(false)
                        .status("COMPILE_ERROR")
                        .error(error)
                        .build());
                return results;
            }

            // 1. 기본 테스트 케이스 실행
            if (problem.getTestCases() != null) {
                for (TestCaseResponse tc : problem.getTestCases()) {
                    results.add(runTestCase(tempDir, tc.getInputData(), tc.getExpectedOutput(), problem.getTimeLimitMs(), problem.getMemoryLimitMb()));
                }
            }

            // 2. 사용자가 추가한 커스텀 테스트 케이스 실행
            if (customTestCases != null) {
                for (CustomTestCaseRequest ctc : customTestCases) {
                    results.add(runTestCase(tempDir, ctc.getInput(), ctc.getExpected(), problem.getTimeLimitMs(), problem.getMemoryLimitMb()));
                }
            }

            if (results.isEmpty()) {
                results.add(ExecutionResult.builder()
                        .success(false)
                        .status("SYSTEM_ERROR")
                        .error("실행할 테스트 케이스가 없습니다.")
                        .build());
            }

        } catch (Exception e) {
            log.error("Execution error", e);
            results.add(ExecutionResult.builder()
                    .success(false)
                    .status("SYSTEM_ERROR")
                    .error(e.getMessage())
                    .build());
        } finally {
            if (tempDir != null) {
                try {
                    Files.walk(tempDir).sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
                } catch (IOException ignored) {}
            }
        }
        return results;
    }

    private ExecutionResult runTestCase(Path dir, String input, String expected, int timeLimitMs, int memoryLimitMb) {
        long startTime = System.currentTimeMillis();
        try {
            ProcessBuilder runBuilder = new ProcessBuilder("java", "-Xmx" + memoryLimitMb + "m", "Solution");
            runBuilder.directory(dir.toFile());
            Process process = runBuilder.start();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(input);
                writer.newLine();
                writer.flush();
            }

            boolean finished = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return ExecutionResult.builder().success(false).status("TIMEOUT").executionTime(executionTime).build();
            }

            if (process.exitValue() != 0) {
                String error = new String(process.getErrorStream().readAllBytes());
                return ExecutionResult.builder().success(false).status("RUNTIME_ERROR").error(error).executionTime(executionTime).build();
            }

            String output = new String(process.getInputStream().readAllBytes()).trim();
            boolean isCorrect = output.equals(expected != null ? expected.trim() : "");

            return ExecutionResult.builder()
                    .success(isCorrect)
                    .status(isCorrect ? "CORRECT" : "WRONG")
                    .output(output)
                    .executionTime(executionTime)
                    .build();

        } catch (Exception e) {
            return ExecutionResult.builder().success(false).status("SYSTEM_ERROR").error(e.getMessage()).build();
        }
    }
}
