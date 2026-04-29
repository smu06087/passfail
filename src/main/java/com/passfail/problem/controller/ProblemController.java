package com.passfail.problem.controller;

import com.passfail.entity.SubmissionEntity;
import com.passfail.enums.ProgrammingLanguage;
import com.passfail.problem.dto.ProblemDTO;
import com.passfail.problem.dto.ProblemResponse;
import com.passfail.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/problem")
@RequiredArgsConstructor
public class ProblemController {

    private static final Logger log = LoggerFactory.getLogger(ProblemController.class);
    private final ProblemService problemService;

    // --- Methods from original ProblemController (User interaction) ---

    @GetMapping
    public String problemList(Model model) {
        List<ProblemResponse> problems = problemService.getActiveProblems();
        model.addAttribute("problems", problems);
        return "problem/problemList";
    }

    // --- Methods from ProblemController1 (Admin / Problem Management) ---

    @GetMapping("/problemList")
    public String list(Authentication authentication, Model model) {
        List<ProblemDTO> problems = problemService.getProblemList();
        ProblemDTO selectedProblem = problems.isEmpty() ? null : problems.get(0);

        model.addAttribute("problems", problems);
        model.addAttribute("problemCount", problems.size());
        model.addAttribute("selectedProblem", selectedProblem);
        model.addAttribute("currentUsername", resolveDisplayName(authentication));
        model.addAttribute("loggedIn", isLoggedIn(authentication));
        model.addAttribute("isAdmin", isAdmin(authentication));
        return "problem/problemList";
    }

    @GetMapping("/problemCreate")
    public String create(Authentication authentication, Model model) {
        populateFormModel(model, authentication, "create", null);
        return "problem/problemCreate";
    }

    @GetMapping("/main")
    public String main(Authentication authentication, Model model) {
        model.addAttribute("currentUsername", resolveDisplayName(authentication));
        model.addAttribute("loggedIn", isLoggedIn(authentication));
        model.addAttribute("isAdmin", isAdmin(authentication));
        return "problem/main";
    }

    @GetMapping("/problemEdit/{problemId}")
    public String edit(@PathVariable Long problemId, Authentication authentication, Model model) {
        populateFormModel(model, authentication, "edit", problemService.getProblemDetail(problemId));
        return "problem/problemCreate";
    }

    @GetMapping("/api/problems/debug")
    public ResponseEntity<Map<String, Object>> debugProblems() {
        return ResponseEntity.ok(problemService.getProblemDebugInfo());
    }

    @GetMapping("/api/problems/search")
    public ResponseEntity<Map<String, Object>> searchProblems(@RequestParam(name = "q", defaultValue = "") String query) {
        return ResponseEntity.ok(problemService.searchProblems(query));
    }

    @PostMapping("/api/problems")
    public ResponseEntity<Map<String, Object>> createProblem(@RequestBody ProblemDTO problemDTO, Principal principal) {
        try {
            if (problemDTO.getCreatedBy() == null && principal != null) {
                problemDTO.setCreatedBy(problemService.resolveCreatedBy(principal.getName()));
            }

            Long problemId = problemService.createProblem(problemDTO);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "problemId", problemId,
                "message", "문제가 저장되었습니다."
            ));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Failed to create problem", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "문제 저장 중 오류가 발생했습니다: " + ex.getClass().getSimpleName() + " - " + ex.getMessage()
            ));
        }
    }

    @PutMapping("/api/problems/{problemId}")
    public ResponseEntity<Map<String, Object>> updateProblem(
        @PathVariable Long problemId,
        @RequestBody ProblemDTO problemDTO
    ) {
        try {
            Long updatedProblemId = problemService.updateProblem(problemId, problemDTO);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "problemId", updatedProblemId,
                "message", "문제가 수정되었습니다."
            ));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Failed to update problem {}", problemId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "문제 수정 중 오류가 발생했습니다: " + ex.getClass().getSimpleName() + " - " + ex.getMessage()
            ));
        }
    }

    // --- Helper Methods ---

    private void populateFormModel(Model model, Authentication authentication, String formMode, ProblemDTO problem) {
        model.addAttribute("currentUsername", resolveDisplayName(authentication));
        model.addAttribute("loggedIn", isLoggedIn(authentication));
        model.addAttribute("isAdmin", isAdmin(authentication));
        model.addAttribute("formMode", formMode);
        model.addAttribute("problemData", problem);
    }

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean isAdmin(Authentication authentication) {
        if (!isLoggedIn(authentication)) return false;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) return true;
        }
        return false;
    }

    private String resolveDisplayName(Authentication authentication) {
        if (!isLoggedIn(authentication)) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oAuth2User) {
            Object name = oAuth2User.getAttribute("name");
            if (name instanceof String value && !value.isBlank()) return value;
            Object login = oAuth2User.getAttribute("login");
            if (login instanceof String value && !value.isBlank()) return value;
            Object response = oAuth2User.getAttribute("response");
            if (response instanceof Map<?, ?> responseMap) {
                Object nickname = responseMap.get("nickname");
                if (nickname instanceof String value && !value.isBlank()) return value;
                Object responseName = responseMap.get("name");
                if (responseName instanceof String value && !value.isBlank()) return value;
            }
        }
        return authentication.getName();
    }
}
