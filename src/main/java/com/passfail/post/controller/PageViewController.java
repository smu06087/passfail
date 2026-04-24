package com.passfail.post.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// ✅ @RestController가 아닌 @Controller → String 반환 시 뷰 이름으로 처리
// Thymeleaf가 templates/post/post-list.html을 렌더링
@Controller
public class PageViewController {

    /**
     * GET /board → templates/post/post-list.html 렌더링
     * 브라우저에서 /board 로 진입하면 게시판 목록 화면이 뜸
     */
    @GetMapping("/board")
    public String postListPage() {
        return "post/post-list"; // templates/post/post-list.html
    }
}