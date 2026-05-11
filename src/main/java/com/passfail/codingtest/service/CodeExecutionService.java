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

/**
 * 사용자가 작성한 코드를 실제로 실행하고 결과를 채점하는 서비스 클래스
 * 로컬 파일 시스템에 임시 디렉토리를 생성하여 컴파일 및 실행을 수행합니다.
 */
@Service
@Slf4j
public class CodeExecutionService {

    /**
     * Java 코드를 컴파일하고 실행함
     * @param problem 문제 정보 (시간/메모리 제한 포함)
     * @param code 실행할 Java 소스 코드
     * @param customTestCases 사용자가 추가한 커스텀 테스트 케이스 목록
     * @return 각 테스트 케이스별 실행 결과 목록
     */
    public List<ExecutionResult> executeJava(ProblemResponse problem, String code, List<CustomTestCaseRequest> customTestCases) {
        List<ExecutionResult> results = new ArrayList<>();
        Path tempDir = null;
        try {
            // 1. 임시 디렉토리 생성 및 소스 파일 저장
            tempDir = Files.createTempDirectory("codingtest_");
            Path sourcePath = tempDir.resolve("Solution.java");
            Files.writeString(sourcePath, code);

            // 2. 컴파일 단계 (javac 실행)
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", "Solution.java");
            compileBuilder.directory(tempDir.toFile());
            Process compileProcess = compileBuilder.start();
            boolean compiled = compileProcess.waitFor(10, TimeUnit.SECONDS);

            // 컴파일 실패 처리
            if (!compiled || compileProcess.exitValue() != 0) {
                String error = new String(compileProcess.getErrorStream().readAllBytes());
                results.add(ExecutionResult.builder()
                        .success(false)
                        .status("COMPILE_ERROR")
                        .error(error)
                        .build());
                return results;
            }

            // 3. 기본 테스트 케이스 실행
            if (problem.getTestCases() != null) {
                for (TestCaseResponse tc : problem.getTestCases()) {
                    results.add(runTestCase(tempDir, tc.getInputData(), tc.getExpectedOutput(), problem.getTimeLimitMs(), problem.getMemoryLimitMb()));
                }
            }

            // 4. 커스텀 테스트 케이스 실행
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
            // 5. 임시 디렉토리 및 파일 삭제 (정리)
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

    /**
     * 개별 테스트 케이스를 실행함
     * @param dir 실행 디렉토리 (Solution.class가 있는 곳)
     * @param input 입력 데이터
     * @param expected 기대하는 출력값
     * @param timeLimitMs 시간 제한 (ms)
     * @param memoryLimitMb 메모리 제한 (MB)
     * @return 실행 결과 DTO
     */
    private ExecutionResult runTestCase(Path dir, String input, String expected, int timeLimitMs, int memoryLimitMb) {
        long startTime = System.currentTimeMillis();
        try {
            // JVM 옵션을 통해 메모리 제한 설정 (-Xmx)
            ProcessBuilder runBuilder = new ProcessBuilder("java", "-Xmx" + memoryLimitMb + "m", "Solution");
            runBuilder.directory(dir.toFile());
            Process process = runBuilder.start();

            // 표준 입력을 통해 데이터 전달
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(input);
                writer.newLine();
                writer.flush();
            }

            // 시간 제한 내에 종료되는지 대기
            boolean finished = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly(); // 시간 초과 시 강제 종료
                return ExecutionResult.builder().success(false).status("TIMEOUT").executionTime(executionTime).build();
            }

            // 런타임 에러 체크
            if (process.exitValue() != 0) {
                String error = new String(process.getErrorStream().readAllBytes());
                return ExecutionResult.builder().success(false).status("RUNTIME_ERROR").error(error).executionTime(executionTime).build();
            }

            // 표준 출력 결과 읽기 및 정답 비교
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
