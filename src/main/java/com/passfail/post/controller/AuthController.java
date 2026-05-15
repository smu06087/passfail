package com.passfail.post.controller;

import com.passfail.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberRepository memberRepository;

    // GET /api/auth/status → 로그인 여부 및 유저네임 반환
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> authStatus(Authentication authentication) {
        boolean loggedIn = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(authentication.getPrincipal()));
        
        if (!loggedIn) {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }

        return ResponseEntity.ok(Map.of(
            "loggedIn", true,
            "username", authentication.getName(),
            "authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList(),
            "dbRole", memberRepository.findByUsername(authentication.getName())
                .map(member -> member.getRole().name())
                .orElse("NOT_FOUND")
        ));
    }
}
