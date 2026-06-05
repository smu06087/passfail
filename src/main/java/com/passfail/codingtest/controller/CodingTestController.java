package com.passfail.codingtest.controller;

import com.passfail.codingtest.dto.ExecutionResult;
import com.passfail.codingtest.dto.CustomTestCaseRequest;
import com.passfail.codingtest.service.CodeExecutionService;
import com.passfail.codingtest.service.CodeExecutionOnDockerService;
import com.passfail.codingtest.service.JudgeSseService;
import com.passfail.codingtest.util.JudgeEnvironmentProvider;
import com.passfail.codingtest.util.DefaultCodeProvider;
import com.passfail.entity.MemberEntity;
import com.passfail.enums.ProgrammingLanguage;
import com.passfail.enums.SubmissionStatus;
import com.passfail.member.repository.MemberRepository;
import com.passfail.problem.dto.ProblemResponse;
import com.passfail.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 코딩 테스트 진행 및 코드 실행/제출을 처리하는 컨트롤러 클래스
 */
@Controller
@RequestMapping("/codingtest")
@RequiredArgsConstructor
@Slf4j
public class CodingTestController {

    private final ProblemService problemService;
    private final CodeExecutionService executionService;
    private final com.passfail.ai.service.AiCodeReviewService aiService;
    private final com.passfail.payment.service.PaymentService paymentService;

    // OnDocker: 서비스 및 유틸리티
    private final CodeExecutionOnDockerService onDockerService;
    private final JudgeSseService sseService;
    private final JudgeEnvironmentProvider envProvider;
    private final DefaultCodeProvider codeProvider;
    private final MemberRepository memberRepository;

    @ModelAttribute("isLinux")
    public boolean isLinux() {
        return envProvider.isLinux();
    }

    /**
     * 배틀 모드(ROGUE) 전용 코딩 테스트 페이지
     */
    @GetMapping("/battle")
    public String battleCodingTestPage(@RequestParam("roomId") Long roomId,
                                       @RequestParam("floor") int floor,
                                       @RequestParam("nodeId") String nodeId,
                                       @RequestParam("seed") Long seed,
                                       Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        ProblemResponse problem = problemService.getBattleProblem(roomId, floor, seed, nodeId);
        MemberEntity member = memberRepository.findByUsername(principal.getName()).orElseThrow();

        model.addAttribute("problem", problem);
        model.addAttribute("roomId", roomId);
        model.addAttribute("seed", seed);
        model.addAttribute("nodeId", nodeId);
        model.addAttribute("currentUserId", member.getMemberId());

        // 기본 정보 (코딩테스트 에디터 공통)
        model.addAttribute("defaultCode", codeProvider.getDefaultCode(ProgrammingLanguage.JAVA));
        boolean isSolved = problemService.isSolved(principal.getName(), problem.getProblemId());
        model.addAttribute("isSolved", isSolved);
        if (isSolved) {
            String previousCode = problemService.getPreviousSolution(principal.getName(), problem.getProblemId());
            model.addAttribute("previousCode", previousCode);
        }

        return "codingtest/rogueModeEditor";
    }

    /**
     * 작성한 코드에 대해 AI 코드 리뷰를 요청하는 API
     */
    @PostMapping("/{problemId}/ai-review")
    @ResponseBody
    public Map<String, String> aiReview(@PathVariable("problemId") Long problemId, 
                                        @RequestBody Map<String, Object> payload,
                                        Principal principal) {
        if (principal == null) {
            return Map.of("review", "❌ 로그인이 필요합니다.");
        }

        try {
            paymentService.useReviewPoints(principal.getName());
            
            String code = (String) payload.get("code");
            Object resultsObj = payload.get("results");
            
            List<ExecutionResult> results = List.of();
            if (resultsObj instanceof List) {
                List<Map<String, Object>> rawResults = (List<Map<String, Object>>) resultsObj;
                results = rawResults.stream()
                        .map(r -> {
                            Object timeObj = r.get("executionTime");
                            long time = (timeObj instanceof Number) ? ((Number) timeObj).longValue() : 0L;
                            String status = (String) r.get("status");
                            return ExecutionResult.builder()
                                    .executionTime(time)
                                    .status(status != null ? status : "UNKNOWN")
                                    .success("CORRECT".equals(status))
                                    .build();
                        })
                        .toList();
            }

            String review = aiService.generateReview(code, results);
            return Map.of("review", review);
        } catch (RuntimeException e) {
            return Map.of("review", "❌ 오류: " + e.getMessage());
        }
    }

    /**
     * 코딩 테스트 에디터 페이지로 이동
     */
    @GetMapping("/{problemId}")
    public String codingTestPage(@PathVariable("problemId") Long problemId, Model model, Principal principal) {
        ProblemResponse problem = problemService.getProblemResponse(problemId);
        model.addAttribute("problem", problem);
        
        // OnDocker: DefaultCodeProvider를 통한 다국어 템플릿 제공
        model.addAttribute("defaultCode", codeProvider.getDefaultCode(ProgrammingLanguage.JAVA));
        
        if (principal != null) {
            String username = principal.getName();
            boolean isSolved = problemService.isSolved(username, problemId);
            model.addAttribute("isSolved", isSolved);
            if (isSolved) {
                String previousCode = problemService.getPreviousSolution(username, problemId);
                model.addAttribute("previousCode", previousCode);
            }
        }

        return "codingtest/editor";
    }

    /**
     * 코드 실행 API (Run)
     */
    @PostMapping("/{problemId}/run")
    @ResponseBody
    public Object runCode(@PathVariable("problemId") Long problemId, 
                          @RequestBody Map<String, Object> payload,
                          Principal principal,
                          @RequestParam(value = "mode", required = false) String mode) {
        
        String langStr = (String) payload.getOrDefault("language", "JAVA");
        ProgrammingLanguage language = ProgrammingLanguage.valueOf(langStr.toUpperCase());
        String code = (String) payload.get("code");

        // LogicMaze 모드일 경우 서버측 구현부 주입
        if ("LOGIC_MAZE".equals(mode)) {
            code += codeProvider.getLogicMazeImplementation(language);
        }

        if (envProvider.isLinux()) {
            try {
                List<Map<String, String>> customCasesRaw = (List<Map<String, String>>) payload.get("customTestCases");
                List<CustomTestCaseRequest> customTestCases = customCasesRaw != null ? 
                    customCasesRaw.stream().map(m -> new CustomTestCaseRequest(m.get("input"), m.get("expected"))).toList() : null;
                
                String id = onDockerService.prepareRun(problemId, code, customTestCases, language);
                return Map.of("id", id);
            } catch (Exception e) {
                return Map.of("error", e.getMessage());
            }
        }

        List<Map<String, String>> customCasesRaw = (List<Map<String, String>>) payload.get("customTestCases");
        List<CustomTestCaseRequest> customTestCases = null;
        if (customCasesRaw != null) {
            customTestCases = customCasesRaw.stream()
                    .map(m -> new CustomTestCaseRequest(m.get("input"), m.get("expected")))
                    .toList();
        }

        ProblemResponse problem = problemService.getProblemResponse(problemId);
        return executionService.execute(problem, code, customTestCases, language);
    }

    /**
     * 최종 코드 제출 API (Submit)
     */
    @PostMapping("/{problemId}/submit")
    @ResponseBody
    public Map<String, Object> submitCode(@PathVariable("problemId") Long problemId, 
                                          @RequestBody Map<String, Object> payload, 
                                          Principal principal,
                                          @RequestParam(value = "mode", required = false) String mode) {

        String langStr = (String) payload.getOrDefault("language", "JAVA");
        ProgrammingLanguage language = ProgrammingLanguage.valueOf(langStr.toUpperCase());
        String code = (String) payload.get("code");

        // LogicMaze 모드일 경우 서버측 구현부 주입
        if ("LOGIC_MAZE".equals(mode)) {
            code += codeProvider.getLogicMazeImplementation(language);
        }

        if (envProvider.isLinux()) {
            try {
                String id = onDockerService.prepareSubmit(problemId, code, principal.getName(), language);
                return Map.of("id", id);
            } catch (Exception e) {
                return Map.of("error", e.getMessage());
            }
        }

        try {
            String username = principal.getName();
            
            ProblemResponse problem = problemService.getProblemResponse(problemId);
            List<ExecutionResult> results = executionService.execute(problem, code, null, language);
            
            boolean allCorrect = results.stream().allMatch(r -> "CORRECT".equals(r.getStatus()));
            
            SubmissionStatus status;
            if (allCorrect) {
                status = SubmissionStatus.ACCEPTED;
            } else {
                status = results.stream()
                        .filter(r -> !"CORRECT".equals(r.getStatus()))
                        .map(r -> switch (r.getStatus()) {
                            case "COMPILE_ERROR" -> SubmissionStatus.COMPILE_ERROR;
                            case "TIMEOUT" -> SubmissionStatus.TIME_LIMIT;
                            case "RUNTIME_ERROR" -> SubmissionStatus.RUNTIME_ERROR;
                            case "WRONG" -> SubmissionStatus.WRONG_ANSWER;
                            default -> SubmissionStatus.WRONG_ANSWER;
                        })
                        .findFirst()
                        .orElse(SubmissionStatus.WRONG_ANSWER);
            }

            problemService.recordSubmission(username, problemId, code, language, status);
            
            return Map.of("allCorrect", allCorrect, "results", results);
        } catch (Exception e) {
            return Map.of("allCorrect", false, "error", e.getMessage() != null ? e.getMessage() : "Unknown Error");
        }
    }

    /**
     * 언어 및 모드별 기본 템플릿 코드를 가져오는 API
     */
    @GetMapping("/template")
    @ResponseBody
    public Map<String, String> getTemplate(@RequestParam("lang") String lang,
                                           @RequestParam(value = "mode", required = false) String mode) {
        try {
            ProgrammingLanguage language = ProgrammingLanguage.valueOf(lang.toUpperCase());
            String template;
            if ("LOGIC_MAZE".equals(mode)) {
                template = codeProvider.getLogicMazeCode(language);
            } else {
                template = codeProvider.getDefaultCode(language);
            }
            return Map.of("template", template);
        } catch (Exception e) {
            return Map.of("template", "");
        }
    }

    @GetMapping(value = "/subscribe/{id}", produces = "text/event-stream")
    @ResponseBody
    public SseEmitter subscribe(@PathVariable String id) {
        if (!envProvider.isLinux()) {
            log.warn("OnDocker: SSE is optimized for Linux. Current OS: {}", envProvider.getOsInfo());
        }
        return sseService.subscribe(id);
    }
}
