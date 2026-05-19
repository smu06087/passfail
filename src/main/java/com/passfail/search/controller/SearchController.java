package com.passfail.search.controller;

import com.passfail.post.dto.PostListResponseDTO;
import com.passfail.post.service.PostService;
import com.passfail.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final ProblemService problemService;
    private final PostService postService;

    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false) String query, Model model) {
        if (query == null || query.trim().isEmpty()) {
            model.addAttribute("problems", java.util.Collections.emptyList());
            model.addAttribute("posts", java.util.Collections.emptyList());
            model.addAttribute("query", "");
            return "search/results";
        }

        String trimmedQuery = query.trim();
        
        // Search problems
        Map<String, Object> problemResults = problemService.searchProblems(trimmedQuery, false);
        
        // Search posts (top 10 results for now)
        var postResults = postService.searchPosts(null, trimmedQuery, PageRequest.of(0, 10));

        model.addAttribute("problems", problemResults.get("problems"));
        model.addAttribute("posts", postResults.getContent());
        model.addAttribute("query", trimmedQuery);
        
        return "search/results";
    }
}
