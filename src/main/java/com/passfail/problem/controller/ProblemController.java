package com.passfail.problem.controller;

import com.passfail.entity.SubmissionEntity;
import com.passfail.enums.ProgrammingLanguage;
import com.passfail.problem.dto.ProblemResponse;
import com.passfail.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    public String problemList(Model model) {
        List<ProblemResponse> problems = problemService.getActiveProblems();
        model.addAttribute("problems", problems);
        return "problem/list";
    }

    @GetMapping("/{id}")
    public String problemDetail(@PathVariable("id") Long id, Model model) {
        ProblemResponse problem = problemService.getProblemResponse(id);
        model.addAttribute("problem", problem);
        return "problem/detail";
    }

    @PostMapping("/{id}/submit")
    public String submitSolution(@PathVariable("id") Long id,
                                 @RequestParam("code") String code,
                                 @RequestParam("language") ProgrammingLanguage language,
                                 Authentication authentication,
                                 Model model) {
        if (authentication == null) return "redirect:/login";

        SubmissionEntity submission = problemService.submitSolution(authentication.getName(), id, code, language);
        model.addAttribute("submission", submission);
        return "problem/result";
    }
}
