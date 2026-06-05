package com.passfail.codingtest.service;

import com.passfail.codingtest.dto.ExecutionResult;
import com.passfail.codingtest.dto.CustomTestCaseRequest;
import com.passfail.enums.ProgrammingLanguage;
import com.passfail.problem.dto.ProblemResponse;
import com.passfail.problem.dto.TestCaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 로컬 OS 환경에서 프로세스를 직접 생성하여 코드를 실행하는 서비스 (V1)
 * Windows 개발 환경에서 Docker 없이 Java, Python, C++ 실행을 지원합니다.
 */
@Service
@Slf4j
public class CodeExecutionService {

    /**
     * 통합 코드 실행 메서드
     */
    public List<ExecutionResult> execute(ProblemResponse problem, String code, List<CustomTestCaseRequest> customTestCases, ProgrammingLanguage language) {
        List<ExecutionResult> results = new ArrayList<>();
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("passfail_v1_" + UUID.randomUUID());
            
            // 1. 소스 코드 저장
            saveCode(tempDir, code, language);

            // 2. 컴파일 (컴파일 언어인 경우)
            if (language == ProgrammingLanguage.JAVA || language == ProgrammingLanguage.CPP) {
                String compileError = compile(tempDir, language);
                if (compileError != null) {
                    results.add(ExecutionResult.builder().success(false).status("COMPILE_ERROR").error(compileError).build());
                    return results;
                }
            }

            // 3. 공식 테스트 케이스 실행
            for (TestCaseResponse tc : problem.getTestCases()) {
                results.add(runSingleTest(tempDir, tc.getInputData(), tc.getExpectedOutput(), problem.getTimeLimitMs(), language));
            }

            // 4. 커스텀 테스트 케이스 실행
            if (customTestCases != null) {
                for (CustomTestCaseRequest ctc : customTestCases) {
                    results.add(runSingleTest(tempDir, ctc.getInput(), ctc.getExpected(), problem.getTimeLimitMs(), language));
                }
            }

        } catch (Exception e) {
            log.error("V1 Execution error", e);
            results.add(ExecutionResult.builder().success(false).status("SYSTEM_ERROR").error(e.getMessage()).build());
        } finally {
            deleteDirectory(tempDir);
        }

        return results;
    }

    private void saveCode(Path dir, String code, ProgrammingLanguage lang) throws IOException {
        String fileName = switch (lang) {
            case JAVA -> "Solution.java";
            case PYTHON -> "main.py";
            case CPP -> "main.cpp";
            default -> "code.txt";
        };
        Files.writeString(dir.resolve(fileName), code);
    }

    private String compile(Path dir, ProgrammingLanguage lang) throws Exception {
        ProcessBuilder pb = switch (lang) {
            case JAVA -> new ProcessBuilder("javac", "Solution.java");
            case CPP -> new ProcessBuilder("g++", "-O2", "main.cpp", "-o", "main.exe");
            default -> null;
        };

        if (pb == null) return null;
        
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder errorOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) errorOutput.append(line).append("\n");
            
            int exitCode = process.waitFor();
            return (exitCode == 0) ? null : errorOutput.toString();
        }
    }

    private ExecutionResult runSingleTest(Path dir, String input, String expected, int timeoutMs, ProgrammingLanguage lang) {
        ProcessBuilder pb = switch (lang) {
            case JAVA -> new ProcessBuilder("java", "-cp", ".", "Solution");
            case PYTHON -> new ProcessBuilder("py", "main.py"); // 윈도우에서는 py 런처가 더 정확함
            case CPP -> new ProcessBuilder("./main.exe");
            default -> throw new RuntimeException("Unsupported language");
        };

        pb.directory(dir.toFile());
        pb.redirectErrorStream(true); // 표준 에러를 표준 출력으로 통합
        long startTime = System.currentTimeMillis();

        try {
            Process process = pb.start();

            // 입력 전달
            if (input != null && !input.isEmpty()) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                    writer.write(input);
                    writer.flush();
                } catch (IOException e) {
                    // 프로세스가 입력을 읽지 않고 일찍 종료된 경우 (예: 문법 오류, 즉시 런타임 에러) 
                    // "파이프가 닫히는 중입니다" 에러 발생 가능. 무시하고 결과 확인 진행.
                    log.warn("입력 전달 중 에러 발생 (프로세스가 일찍 종료되었을 수 있음): {}", e.getMessage());
                }
            }

            boolean finished = process.waitFor(timeoutMs + 500, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return ExecutionResult.builder().success(false).status("TIMEOUT").executionTime(executionTime).build();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) output.append(line).append("\n");
                
                String actualOutput = output.toString().trim();
                boolean success = actualOutput.equals(expected != null ? expected.trim() : "");
                
                return ExecutionResult.builder()
                        .success(success)
                        .status(success ? "CORRECT" : "WRONG")
                        .output(actualOutput)
                        .executionTime(executionTime)
                        .build();
            }

        } catch (Exception e) {
            return ExecutionResult.builder().success(false).status("RUNTIME_ERROR").error(e.getMessage()).build();
        }
    }

    private void deleteDirectory(Path dir) {
        if (dir == null) return;
        try {
            Files.walk(dir).sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }
}
