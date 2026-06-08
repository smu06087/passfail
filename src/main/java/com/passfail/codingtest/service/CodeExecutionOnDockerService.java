package com.passfail.codingtest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passfail.codingtest.dto.CustomTestCaseRequest;
import com.passfail.codingtest.dto.ExecutionResult;
import com.passfail.codingtest.util.DockerRunner;
import com.passfail.entity.MemberEntity;
import com.passfail.entity.ProblemEntity;
import com.passfail.entity.SubmissionEntity;
import com.passfail.entity.TestCaseEntity;
import com.passfail.enums.ProgrammingLanguage;
import com.passfail.enums.SubmissionStatus;
import com.passfail.member.repository.MemberRepository;
import com.passfail.problem.repository.ProblemRepository;
import com.passfail.problem.repository.SubmissionRepository;
import com.passfail.problem.repository.TestCaseRepository;
import com.passfail.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Docker 기반 격리 실행 및 병렬 채점을 지원하는 향상된 채점 서비스 (OnDocker)
 * - 비즈니스 로직(DB 등록, 큐 등록)과 채점 로직(Worker)을 통합 관리합니다.
 */
@Service
@Slf4j
public class CodeExecutionOnDockerService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final MemberRepository memberRepository;
    private final ProblemService problemService;
    private final DockerRunner dockerRunner;
    private final JudgeSseService sseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RedisTemplate<String, String> stringRedisTemplateCustom;

    private final ExecutorService judgePool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public CodeExecutionOnDockerService(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            TestCaseRepository testCaseRepository,
            MemberRepository memberRepository,
            ProblemService problemService,
            DockerRunner dockerRunner,
            JudgeSseService sseService,
            @Qualifier("stringRedisTemplateCustom") RedisTemplate<String, String> stringRedisTemplateCustom) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.memberRepository = memberRepository;
        this.problemService = problemService;
        this.dockerRunner = dockerRunner;
        this.sseService = sseService;
        this.stringRedisTemplateCustom = stringRedisTemplateCustom;
    }

    // --- OnDocker: 요청 엔트리 포인트 (Controller 호출용) ---

    @Transactional
    public String prepareSubmit(Long problemId, String code, String username, ProgrammingLanguage language) throws Exception {
        MemberEntity member = memberRepository.findByUsername(username).orElseThrow();

        SubmissionEntity submission = SubmissionEntity.builder()
                .memberId(member.getMemberId())
                .problemId(problemId)
                .code(code)
                .language(language)
                .status(SubmissionStatus.PENDING)
                .build();

        SubmissionEntity saved = submissionRepository.save(submission);
        String id = saved.getSubmissionId().toString();

        Map<String, Object> messageMap = Map.of(
                "id", id,
                "mode", "SUBMIT",
                "problemId", problemId,
                "code", code,
                "language", language.name()
        );
        log.info("OnDocker: prepareSubmit ID: {}. Lang: {}", id,  language);
        stringRedisTemplateCustom.opsForList().rightPush("judge_queue", objectMapper.writeValueAsString(messageMap));
        return id;
    }

    public String prepareRun(Long problemId, String code, List<CustomTestCaseRequest> customCases, ProgrammingLanguage language) throws Exception {
        String runId = "run_" + UUID.randomUUID().toString();

        Map<String, Object> messageMap = Map.of(
                "id", runId,
                "mode", "RUN",
                "problemId", problemId,
                "code", code,
                "language", language.name(),
                "customTestCases", customCases != null ? customCases : List.of()
        );

        stringRedisTemplateCustom.opsForList().rightPush("judge_queue", objectMapper.writeValueAsString(messageMap));
        log.info("OnDocker: prepareRun ID: {}. Lang: {}", runId,  language);
        return runId;
    }

    // --- OnDocker: 채점 워커 로직 (Listener 호출용) ---

    /**
     * 레거시 단순 ID 요청 처리
     */
    public void processLegacySubmit(Long submissionId) {
        submissionRepository.findById(submissionId).ifPresent(s -> {
            judgeAsync(s.getSubmissionId().toString(), s.getProblemId(), s.getCode(), s.getLanguage());
        });
    }

    public void judgeAsync(String id, Long problemId, String code, ProgrammingLanguage language) {
        processInternal(id, problemId, code, null, true, language);
    }

    public void runAsync(String id, Long problemId, String code, List<CustomTestCaseRequest> customTestCases, ProgrammingLanguage language) {
        processInternal(id, problemId, code, customTestCases, false, language);
    }

    private void processInternal(String id, Long problemId, String code, List<CustomTestCaseRequest> customTestCases, boolean isSubmit, ProgrammingLanguage language) {
        log.info("OnDocker: Processing async job for ID: {}. Mode: {}, Lang: {}", id, isSubmit ? "SUBMIT" : "RUN", language);
        sseService.sendStatus(id, "실행 준비 중...");

        ProblemEntity problem = problemRepository.findById(problemId).orElse(null);
        if (problem == null) {
            sseService.complete(id, "{\"error\": \"PROBLEM_NOT_FOUND\"}");
            updateSubmissionStatus(id, isSubmit, SubmissionStatus.SYSTEM_ERROR, 0, 0);
            return;
        }

        List<TestCaseEntity> testCases = testCaseRepository.findByProblem_ProblemId(problemId);
        
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("judge_on_docker_" + id + "_");
            sseService.sendStatus(id, "코드를 컴파일하는 중...");
            saveCode(workDir, language, code);
            
            boolean compileSuccess = preCompile(workDir, language, problem);
            if (!compileSuccess) {
                log.error("OnDocker: 컴파일 실패로 채점 중단");
                sseService.complete(id, "{\"error\": \"COMPILE_ERROR\"}");
                updateSubmissionStatus(id, isSubmit, SubmissionStatus.COMPILE_ERROR, 0, 0);
                return;
            }

            final Path finalWorkDir = workDir;
            List<CompletableFuture<ExecutionResult>> futures = new ArrayList<>();

            for (int i = 0; i < testCases.size(); i++) {
                final int index = i + 1;
                final TestCaseEntity tc = testCases.get(i);
                futures.add(submitTask(id, finalWorkDir, language, tc.getInputData(), tc.getExpectedOutput(), problem, "specified_" + index));
            }

            if (!isSubmit && customTestCases != null) {
                for (int i = 0; i < customTestCases.size(); i++) {
                    final int index = i + 1;
                    final CustomTestCaseRequest ctc = customTestCases.get(i);
                    futures.add(submitTask(id, finalWorkDir, language, ctc.getInput(), ctc.getExpected(), problem, "custom_" + index));
                }
            }
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            List<ExecutionResult> results = new ArrayList<>();
            long maxTime = 0;
            int maxMemory = 0;
            SubmissionStatus finalStatus = SubmissionStatus.ACCEPTED;
            boolean allCorrect = true;
           
            for (CompletableFuture<ExecutionResult> future : futures) {
                ExecutionResult result = future.get();
                results.add(result);
                maxTime = Math.max(maxTime, result.getExecutionTime());
                maxMemory = (int) Math.max(maxMemory, result.getMemoryUsed());

                if (!result.isSuccess()) {
                    allCorrect = false;
                    if (finalStatus == SubmissionStatus.ACCEPTED) {
                        finalStatus = mapToSubmissionStatus(result.getStatus());
                    }
                }
            }
            
            updateSubmissionStatus(id, isSubmit, allCorrect ? SubmissionStatus.ACCEPTED : finalStatus, (int) maxTime, maxMemory);

            Map<String, Object> finalResponse = isSubmit ? 
                Map.of("allCorrect", allCorrect, "results", results) : 
                Map.of("results", results);
            
            sseService.complete(id, objectMapper.writeValueAsString(finalResponse));

        } catch (Exception e) {
            log.error("OnDocker: System error", e);
            sseService.complete(id, "{\"error\": \"SYSTEM_ERROR\"}");
            updateSubmissionStatus(id, isSubmit, SubmissionStatus.SYSTEM_ERROR, 0, 0);
        } finally {
            deleteDirectory(workDir);
        }
    }

    private void updateSubmissionStatus(String id, boolean isSubmit, SubmissionStatus status, int timeMs, int memoryKb) {
        if (!isSubmit) return;
        try {
            Long submissionId = Long.parseLong(id);
            problemService.recordSubmissionResult(submissionId, status, timeMs, memoryKb);
            log.info("OnDocker: updated submission id: {} to status: {} via problemService", submissionId, status);
        } catch (Exception e) {
            log.error("Failed to update submission status", e);
        }
    }

    private CompletableFuture<ExecutionResult> submitTask(String id, Path workDir, ProgrammingLanguage lang, String input, String expected, ProblemEntity problem, String label) {
        return CompletableFuture.supplyAsync(() -> {
            try {
            	log.info("OnDocker: runtask testcase label:"+ label);
                sseService.sendStatus(id, label + " 테스트 케이스 실행 중...");
                return runSingleTest(workDir, lang, input, expected, problem, label);
            } catch (Exception e) {
            	log.info("OnDocker: runtask testcase Exception label:"+ label +" e:"+ e.getMessage());
                return ExecutionResult.builder().success(false).status("SYSTEM_ERROR").error(e.getMessage()).build();
            }
        }, judgePool);
    }

    private boolean preCompile(Path workDir, ProgrammingLanguage lang, ProblemEntity problem) throws Exception {
        // 자바나 C++인 경우만 선행 컴파일 필요 (파이썬은 인터프리터 언어라 컴파일 패스)
        if (lang == ProgrammingLanguage.PYTHON) return true;

        String compileCmd = switch (lang) {
            case JAVA -> "javac -encoding UTF-8 Solution.java";
            case CPP -> "g++ -O2 Main.cpp -o main";
            default -> "exit 0";
        };

        List<String> cmd = List.of(
                "docker", "run", "--rm",
                "-v", workDir.toAbsolutePath() + ":/workspace",
                "--workdir=/workspace",
                "-e", "LANG=C.UTF-8",
                "judge-sandbox",
                "/bin/bash", "-c", compileCmd + " 2>&1"
        );

        DockerRunner.DockerResult result = dockerRunner.run(cmd, 20); // 컴파일 타임아웃 20초 제한
        log.info("OnDocker: Pre-Compile Log -> {}", result.getOutput());

        return result.getExitCode() == 0;
    }
    
    private ExecutionResult runSingleTest(Path workDir, ProgrammingLanguage lang, String input, String expected, ProblemEntity problem, String label) throws Exception {
    	String safeLabel = label.replaceAll("\\s+", "");
        String inputFileName = "input_" + safeLabel + ".txt";
    	
        Files.writeString(workDir.resolve(inputFileName), input, StandardCharsets.UTF_8);
        String runCmd = buildCommand(lang, inputFileName);
        
        List<String> cmd = List.of(
                "docker", "run", "--rm",
                "-v", workDir.toAbsolutePath() + ":/workspace",
                "--workdir=/workspace",
                "--network=none",
                "--cpus=0.5",
                "-e", "LANG=C.UTF-8",
                "-e", "PYTHONIOENCODING=utf-8",
                "--memory=" + problem.getMemoryLimitMb() + "m",
                "judge-sandbox",
                "/bin/bash", "-c", runCmd
        );
        
        DockerRunner.DockerResult result = dockerRunner.run(cmd, problem.getTimeLimitMs() / 1000 + 2);
        log.info("OnDocker: runtask testcase result - ExitCode: {}, Timeout: {}", result.getExitCode(), result.isTimeout());
        return parseToExecutionResult(result, expected, problem);
    }

    private ExecutionResult parseToExecutionResult(DockerRunner.DockerResult result, String expected, ProblemEntity problem) {
        String raw = result.getOutput();
        log.info("[OnDocker] Parsing raw output: \n{}", raw);
        
        long timeMs = parseTime(raw);
        int memoryKb = (int) parseMemory(raw);
        
        String status = "CORRECT";
        boolean success = true;

        if (result.isTimeout()) {
            status = "TIMEOUT";
            success = false;
        } else if (result.getExitCode() != 0) {
            status = "RUNTIME_ERROR";
            success = false;
        } else if (timeMs > problem.getTimeLimitMs()) {
            status = "TIMEOUT";
            success = false;
        } else if (memoryKb > problem.getMemoryLimitMb() * 1024) {
            status = "MEMORY_LIMIT_EXCEEDED";
            success = false;
        } else {
            String actualOutput = extractProgramOutput(raw).trim();
            log.info("[OnDocker] Extracted output: [{}]", actualOutput);
            if (!actualOutput.equals(expected != null ? expected.trim() : "")) {
                status = "WRONG";
                success = false;
            }
        }

        return ExecutionResult.builder()
                .success(success)
                .status(status)
                .executionTime(timeMs)
                .memoryUsed(memoryKb)
                .output(extractProgramOutput(raw))
                .build();
    }

    private SubmissionStatus mapToSubmissionStatus(String status) {
        return switch (status) {
            case "COMPILE_ERROR" -> SubmissionStatus.COMPILE_ERROR;
            case "TIMEOUT" -> SubmissionStatus.TIME_LIMIT;
            case "MEMORY_LIMIT_EXCEEDED" -> SubmissionStatus.MEMORY_LIMIT;
            case "RUNTIME_ERROR" -> SubmissionStatus.RUNTIME_ERROR;
            default -> SubmissionStatus.WRONG_ANSWER;
        };
    }

    private String buildCommand(ProgrammingLanguage lang, String inputFileName) {
    	return switch (lang) {
    // 이미 만들어진 Solution.class를 가져다가 실행만 처리
            // JVM 최적화: TieredStopAtLevel=1 (컴파일 오버헤드 감소), Xmx/Xms 제한
    case JAVA -> "cat " + inputFileName + " | /usr/bin/time -v java -Xmx128m -Xms64m -XX:TieredStopAtLevel=1 -cp . Solution";
    // 이미 빌드된 공통 실행 파일 ./main을 여러 스레드가 동시에 켜서 읽기만 수행
    case CPP -> "cat " + inputFileName + " | /usr/bin/time -v ./main";
    case PYTHON -> "cat " + inputFileName + " | /usr/bin/time -v python3 main.py";
    default -> "exit 1";
    };
    }


    private void saveCode(Path dir, ProgrammingLanguage lang, String code) throws Exception {
        Path file = switch (lang) {
            case JAVA -> dir.resolve("Solution.java");
            case PYTHON -> dir.resolve("main.py");
            case CPP -> dir.resolve("Main.cpp");
            default -> throw new RuntimeException("Unsupported language: " + lang);
        };
        Files.writeString(file, code, StandardCharsets.UTF_8);
    }

    private long parseTime(String raw) {
        Pattern p = Pattern.compile("Elapsed \\(wall clock\\) time.*?:\\s*(\\d+):(\\d+\\.\\d+)");
        Matcher m = p.matcher(raw);
        return m.find() ? (long)((Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2))) * 1000) : 0;
    }

    private long parseMemory(String raw) {
        Pattern p = Pattern.compile("Maximum resident set size \\(kbytes\\):\\s*(\\d+)");
        Matcher m = p.matcher(raw);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    private String extractProgramOutput(String raw) {
        int idx = raw.indexOf("Command being timed:");
        return (idx == -1) ? raw.trim() : raw.substring(0, idx).trim();
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
