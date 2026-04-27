package com.passfail.post.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.passfail.entity.MemberEntity;
import com.passfail.post.dto.PostDetailResponseDTO;
import com.passfail.post.service.PostService;

import lombok.RequiredArgsConstructor;

// ✅ @RestController가 아닌 @Controller → String 반환 시 뷰 이름으로 처리
// Thymeleaf가 templates/post/post-list.html을 렌더링
@Controller
@RequiredArgsConstructor
public class PageViewController {
	
	private final PostService postService;
	
    /**
     * GET /board → templates/post/post-list.html 렌더링
     * 브라우저에서 /board 로 진입하면 게시판 목록 화면이 뜸
     */
    @GetMapping("/board")
    public String postListPage() {
        return "post/post-list"; // templates/post/post-list.html
    }
    
    /*
     * ✅ 추가: GET /posts/{postId}/view → 상세 페이지 렌더링
     * 브라우저에서 /posts/5/view 로 접근하면 상세 보기 화면이 뜸
     */
    
    /*
    
    @GetMapping("/posts/{postId}/view")
    public String postDetailPage(
            @PathVariable(name = "postId") Long postId,
            Model model,
            @AuthenticationPrincipal Long currentMemberId) {
        
        try {
            PostDetailResponseDTO post = postService.getPostDetail(postId, currentMemberId);
            model.addAttribute("post", post);
            return "post/post-detail"; // templates/post/post-detail.html
        } catch (Exception e) {
            model.addAttribute("error", "게시글을 찾을 수 없습니다.");
            return "error/404";
        }
    }
    
    */
    
    @GetMapping("/posts/{postId}/view")
    public String postDetailPage(
            @PathVariable(name = "postId") Long postId,
            Model model,
            Authentication authentication) {
        
        Long currentMemberId = null;
        
        // ✅ 안전한 타입 처리
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            System.out.println("🔍 Principal Type: " + principal.getClass().getName());
            System.out.println("🔍 Principal Value: " + principal);
            
            if (principal instanceof MemberEntity) {
                MemberEntity member = (MemberEntity) principal;
                currentMemberId = member.getMemberId();
                System.out.println("✅ MemberEntity로 캐스팅 성공: " + currentMemberId);
            } 
            else if (principal instanceof String && !"anonymousUser".equals(principal)) {
                try {
                    currentMemberId = Long.parseLong((String) principal);
                    System.out.println("✅ String을 Long으로 변환: " + currentMemberId);
                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Username은 숫자가 아님: " + principal);
                }
            }
        }
        
        try {
            PostDetailResponseDTO post = postService.getPostDetail(postId, currentMemberId);
            
            if (post == null) {
                model.addAttribute("error", "게시글을 찾을 수 없습니다.");
                return "error/404";
            }
            
            model.addAttribute("post", post);
            return "post/post-detail";
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "게시글을 로드할 수 없습니다.");
            return "error/404";
        }
    }
    
}