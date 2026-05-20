package com.passfail.problem.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.passfail.payment.service.PaymentService;
import com.passfail.problem.dto.ProblemDTO;
import com.passfail.problem.dto.ProblemResponse;
import com.passfail.problem.service.ProblemPdfService;
import com.passfail.problem.service.ProblemService;

@Controller
@RequestMapping("/problem")
public class ProblemController {

    private static final Logger log = LoggerFactory.getLogger(ProblemController.class);

    private final ProblemService problemService;
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final ProblemPdfService pdfService;

    public ProblemController(
        ProblemService problemService,
        ObjectMapper objectMapper,
        PaymentService paymentService,
        ProblemPdfService pdfService
    ) {
        this.problemService = problemService;
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
        this.pdfService = pdfService;
    }

    @GetMapping("/{problemId}/download")
    public ResponseEntity<?> downloadProblemPdf(@PathVariable("problemId") Long problemId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            paymentService.useDownloadPoints(principal.getName());
            ProblemResponse problem = problemService.getProblemResponse(problemId);
            byte[] pdfBytes = pdfService.generateProblemPdf(problem);

            String filename = "Problem_" + problemId + ".pdf";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
        } catch (RuntimeException ex) {
            log.error("Failed to download PDF due to payment or logic error: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Internal error during PDF generation", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("success", false, "message", "PDF 생성 중 서버 오류가 발생했습니다."));
        }
    }

    @GetMapping
    public String problemList(Authentication authentication, Model model) {
        populateProblemListModel(model, authentication);
        return "problem/problemList";
    }

    @GetMapping("/problemList")
    public String list(Authentication authentication, Model model) {
        populateProblemListModel(model, authentication);
        return "problem/problemList";
    }

    @GetMapping("/problemCreate")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public String edit(@PathVariable("problemId") Long problemId, Authentication authentication, Model model) {
        populateFormModel(model, authentication, "edit", problemService.getProblemDetail(problemId));
        return "problem/problemCreate";
    }

    @GetMapping("/api/problems/debug")
    public ResponseEntity<Map<String, Object>> debugProblems() {
        return ResponseEntity.ok(problemService.getProblemDebugInfo());
    }

    @GetMapping("/api/problems/search")
    public ResponseEntity<Map<String, Object>> searchProblems(
        @RequestParam(name = "q", defaultValue = "") String query,
        Authentication authentication
    ) {
        return ResponseEntity.ok(problemService.searchProblems(query, isAdmin(authentication), resolveLoginName(authentication)));
    }

    @PostMapping("/api/problems")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createProblem(@RequestBody ProblemDTO problemDTO, Principal principal) {
        try {
            if (problemDTO.getCreatedBy() == null && principal != null) {
                problemDTO.setCreatedBy(problemService.resolveCreatedBy(principal.getName()));
            }

            Long problemId = problemService.createProblem(problemDTO);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "problemId", problemId,
                "message", "문제가 등록되었습니다."
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
                "message", "문제 등록 중 오류가 발생했습니다: " + ex.getClass().getSimpleName() + " - " + ex.getMessage()
            ));
        }
    }

    @PutMapping("/api/problems/{problemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateProblem(
        @PathVariable("problemId") Long problemId,
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

    @DeleteMapping("/api/problems/{problemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteProblem(@PathVariable("problemId") Long problemId) {
        try {
            problemService.deleteProblem(problemId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "problemId", problemId,
                "message", "문제가 삭제되었습니다."
            ));
        } catch (IllegalArgumentException | IllegalStateException | jakarta.persistence.EntityNotFoundException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Failed to delete problem {}", problemId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "문제 삭제 중 오류가 발생했습니다: " + ex.getClass().getSimpleName() + " - " + ex.getMessage()
            ));
        }
    }

    private void populateFormModel(Model model, Authentication authentication, String formMode, ProblemDTO problem) {
        model.addAttribute("currentUsername", resolveDisplayName(authentication));
        model.addAttribute("loggedIn", isLoggedIn(authentication));
        model.addAttribute("isAdmin", isAdmin(authentication));
        model.addAttribute("formMode", formMode);
        model.addAttribute("problemData", problem);
        model.addAttribute("problemDataJson", toJson(problem));
    }

    private void populateProblemListModel(Model model, Authentication authentication) {
        boolean admin = isAdmin(authentication);
        List<ProblemDTO> problems = problemService.getProblemList(admin, resolveLoginName(authentication));
        ProblemDTO selectedProblem = problems.isEmpty() ? null : problems.get(0);

        model.addAttribute("problems", problems);
        model.addAttribute("problemCount", problems.size());
        model.addAttribute("selectedProblem", selectedProblem);
        model.addAttribute("currentUsername", resolveDisplayName(authentication));
        model.addAttribute("loggedIn", isLoggedIn(authentication));
        model.addAttribute("isAdmin", admin);
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
        return authentication.getName();
    }

    private String resolveLoginName(Authentication authentication) {
        return isLoggedIn(authentication) ? authentication.getName() : null;
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize model data for problem form", ex);
            return "null";
        }
    }
}
