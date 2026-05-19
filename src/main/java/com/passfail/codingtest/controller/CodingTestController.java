package com.passfail.codingtest.controller;

import com.passfail.codingtest.dto.ExecutionResult;
import com.passfail.codingtest.dto.CustomTestCaseRequest;
import com.passfail.codingtest.service.CodeExecutionService;
import com.passfail.enums.ProgrammingLanguage;
import com.passfail.enums.SubmissionStatus;
import com.passfail.problem.dto.ProblemResponse;
import com.passfail.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 코딩 테스트 진행 및 코드 실행/제출을 처리하는 컨트롤러 클래스
 * 온라인 에디터 페이지 제공, 코드 실행, 채점, AI 코드 리뷰 요청 기능을 담당합니다.
 */
@Controller
@RequestMapping("/codingtest")
@RequiredArgsConstructor
public class CodingTestController {

    private final ProblemService problemService;
    private final CodeExecutionService executionService;
    private final com.passfail.ai.service.AiCodeReviewService aiService;
    private final com.passfail.payment.service.PaymentService paymentService;

    /**
     * 작성한 코드에 대해 AI 코드 리뷰를 요청하는 API
     * @param problemId 문제 ID
     * @param payload 작성한 코드 및 실행 결과 데이터를 포함한 맵
     * @return AI가 생성한 리뷰 텍스트
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
            // 1. 포인트 소모 (2000 바나나)
            paymentService.useReviewPoints(principal.getName());
            
            // 2. 리뷰 생성 로직 시작
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

            // AI 서비스를 통해 리뷰 메시지 생성
            String review = aiService.generateReview(code, results);
            return Map.of("review", review);
        } catch (RuntimeException e) {
            return Map.of("review", "❌ 오류: " + e.getMessage());
        }
    }

    /**
     * 코딩 테스트 에디터 페이지로 이동
     * @param problemId 문제 ID
     * @param model 뷰에 전달할 데이터 모델
     * @param principal 현재 로그인한 사용자 정보
     */
    @GetMapping("/{problemId}")
    public String codingTestPage(@PathVariable("problemId") Long problemId, Model model, Principal principal) {
        ProblemResponse problem = problemService.getProblemResponse(problemId);
        model.addAttribute("problem", problem);
        
        // 에디터 초기 코드 설정
        String defaultCode = "import java.util.*;\n\npublic class Solution {\n" +
                             "    public static void main(String[] args) {\n" +
                             "        Scanner sc = new Scanner(System.in);\n" +
                             "        // 코드를 작성하세요\n" +
                             "    }\n" +
                             "}";
        
        if (principal != null) {
            String username = principal.getName();
            // 문제 해결 여부 확인
            boolean isSolved = problemService.isSolved(username, problemId);
            model.addAttribute("isSolved", isSolved);
            // 이미 해결한 경우 이전에 제출했던 코드 로드
            if (isSolved) {
                String previousCode = problemService.getPreviousSolution(username, problemId);
                model.addAttribute("previousCode", previousCode);
            }
        }

        model.addAttribute("defaultCode", defaultCode);
        return "codingtest/editor";
    }

    /**
     * 작성한 코드를 테스트 케이스와 함께 실행하는 API
     * 공식 테스트 케이스 및 사용자가 추가한 커스텀 테스트 케이스를 모두 실행합니다.
     */
    @PostMapping("/{problemId}/run")
    @ResponseBody
    public List<ExecutionResult> runCode(@PathVariable("problemId") Long problemId, 
                                         @RequestBody Map<String, Object> payload) {
        String code = (String) payload.get("code");
        List<Map<String, String>> customCasesRaw = (List<Map<String, String>>) payload.get("customTestCases");
        
        List<CustomTestCaseRequest> customTestCases = null;
        if (customCasesRaw != null) {
            customTestCases = customCasesRaw.stream()
                    .map(m -> new CustomTestCaseRequest(m.get("input"), m.get("expected")))
                    .toList();
        }

        ProblemResponse problem = problemService.getProblemResponse(problemId);
        // Java 코드를 컴파일 및 실행하여 결과 반환
        return executionService.executeJava(problem, code, customTestCases);
    }

    /**
     * 최종 코드 제출 API
     * 공식 테스트 케이스로만 채점을 진행하며, 모든 케이스 통과 시 정답 처리합니다.
     */
    @PostMapping("/{problemId}/submit")
    @ResponseBody
    public Map<String, Object> submitCode(@PathVariable("problemId") Long problemId, 
                                          @RequestBody Map<String, String> payload, 
                                          Principal principal) {
        try {
            String code = payload.get("code");
            String username = principal.getName();
            
            ProblemResponse problem = problemService.getProblemResponse(problemId);
            // 제출 시에는 커스텀 테스트 케이스 없이 공식 케이스로만 채점
            List<ExecutionResult> results = executionService.executeJava(problem, code, null);
            
            // 모든 테스트 케이스가 정답(CORRECT)인지 확인
            boolean allCorrect = results.stream().allMatch(r -> "CORRECT".equals(r.getStatus()));
            
            // 제출 상태 결정
            SubmissionStatus status;
            if (allCorrect) {
                status = SubmissionStatus.ACCEPTED;
            } else {
                // 첫 번째 실패한 케이스의 상태를 대표 상태로 설정
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

            // 문제 해결 정보 기록 (성공/실패 여부 상관없이 기록됨)
            problemService.recordSubmission(username, problemId, code, ProgrammingLanguage.JAVA, status);
            
            return Map.of("allCorrect", allCorrect, "results", results);
        } catch (Exception e) {
            return Map.of("allCorrect", false, "error", e.getMessage() != null ? e.getMessage() : "Unknown Error");
        }
    }
}
