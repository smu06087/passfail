package com.passfail.codingtest.controller;

import com.passfail.codingtest.dto.ExecutionResult;
import com.passfail.codingtest.dto.CustomTestCaseRequest;
import com.passfail.codingtest.service.CodeExecutionService;
import com.passfail.enums.ProgrammingLanguage;
import com.passfail.problem.dto.ProblemResponse;
import com.passfail.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/codingtest")
@RequiredArgsConstructor
public class CodingTestController {

    private final ProblemService problemService;
    private final CodeExecutionService executionService;
    private final com.passfail.ai.service.AiCodeReviewService aiService;

    @GetMapping
    public String problemListPage(Model model) {
        List<ProblemResponse> problems = problemService.getActiveProblems();
        model.addAttribute("problems", problems);
        return "codingtest/list";
    }

    @PostMapping("/{problemId}/ai-review")
    @ResponseBody
    public Map<String, String> aiReview(@PathVariable("problemId") Long problemId, 
                                        @RequestBody Map<String, Object> payload) {
        String code = (String) payload.get("code");
        List<Map<String, Object>> rawResults = (List<Map<String, Object>>) payload.get("results");
        
        List<ExecutionResult> results = rawResults.stream()
                .map(r -> ExecutionResult.builder()
                        .executionTime(((Number) r.get("executionTime")).longValue())
                        .status((String) r.get("status"))
                        .build())
                .toList();

        String review = aiService.generateReview(code, results);
        return Map.of("review", review);
    }

    @GetMapping("/{problemId}")
    public String codingTestPage(@PathVariable("problemId") Long problemId, Model model, Principal principal) {
        ProblemResponse problem = problemService.getProblemResponse(problemId);
        model.addAttribute("problem", problem);
        
        String defaultCode = "import java.util.*;\n\npublic class Solution {\n" +
                             "    public static void main(String[] args) {\n" +
                             "        Scanner sc = new Scanner(System.in);\n" +
                             "        // 코드를 작성하세요\n" +
                             "    }\n" +
                             "}";
        
        if (principal != null) {
            String username = principal.getName();
            boolean isSolved = problemService.isSolved(username, problemId);
            model.addAttribute("isSolved", isSolved);
            if (isSolved) {
                String previousCode = problemService.getPreviousSolution(username, problemId);
                model.addAttribute("previousCode", previousCode);
            }
        }

        model.addAttribute("defaultCode", defaultCode);
        return "codingtest/editor";
    }

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
        return executionService.executeJava(problem, code, customTestCases);
    }

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
            
            boolean allCorrect = results.stream().allMatch(r -> "CORRECT".equals(r.getStatus()));
            
            if (allCorrect) {
                problemService.submitSolution(username, problemId, code, ProgrammingLanguage.JAVA);
            }
            
            return Map.of("allCorrect", allCorrect, "results", results);
        } catch (Exception e) {
            return Map.of("allCorrect", false, "error", e.getMessage() != null ? e.getMessage() : "Unknown Error");
        }
    }
}
