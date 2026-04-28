package com.passfail.post.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.passfail.post.dto.PostDetailResponseDTO;
import com.passfail.post.service.PostService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PageViewController {

    private final PostService postService;

    @GetMapping("/board")
    public String postListPage() {
        return "post/post-list";
    }

    // ✅ 상세 페이지: username(String)을 서비스로 넘김
    @GetMapping("/posts/{postId}/view")
    public String postDetailPage(
            @PathVariable(name = "postId") Long postId, // 여기도 name 추가
            Model model, 
            Authentication authentication) {
        
        String currentUsername = null;
        if (authentication != null && authentication.isAuthenticated()) {
            currentUsername = authentication.getName(); // 시큐리티에서 유니크한 이름 가져오기
        }

        PostDetailResponseDTO post = postService.getPostDetail(postId, currentUsername);
        model.addAttribute("post", post);
        
        return "post/post-detail";
    }

    // ✅ 글쓰기 페이지
    @GetMapping("/posts/write")
    public String postWritePage(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        return "post/post-write";
    }

    // ✅ 수정 페이지: 여기도 username(String)으로 본인 확인 하도록 수정
    // ✅ 수정 페이지
    @GetMapping("/posts/{postId}/edit")
    public String postEditPage(
            @PathVariable(name = "postId") Long postId,
            Model model,
            Authentication authentication) {

        // 1. 로그인 체크
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        // 2. ID 대신 이름(String)을 가져옴
        String currentUsername = authentication.getName(); 

        try {
            // 3. 서비스 호출 (이제 서비스가 String을 받으므로 에러가 사라짐)
            PostDetailResponseDTO post = postService.getPostDetail(postId, currentUsername);
            
            // 4. 본인 확인
            if (post.getIsAuthor() == null || !post.getIsAuthor()) {
                return "redirect:/posts/" + postId + "/view";
            }
            
            model.addAttribute("post", post);
            return "post/post-edit";
        } catch (Exception e) {
            return "redirect:/board";
        }
    }
}