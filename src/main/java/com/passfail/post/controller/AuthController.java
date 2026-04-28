package com.passfail.post.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // GET /api/auth/status → 로그인 여부 반환
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> authStatus(Authentication authentication) {
        boolean loggedIn = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(authentication.getPrincipal()));
        return ResponseEntity.ok(Map.of("loggedIn", loggedIn));
    }
}