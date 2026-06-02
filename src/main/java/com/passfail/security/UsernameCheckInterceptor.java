package com.passfail.security;

import com.passfail.entity.MemberEntity;
import com.passfail.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * 아이디(닉네임)가 설정되지 않은 소셜 로그인 사용자를 아이디 설정 페이지로 강제 리다이렉트하는 인터셉터
 */
@Component
@RequiredArgsConstructor
public class UsernameCheckInterceptor implements HandlerInterceptor {

    private final MemberRepository memberRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 1. 로그인 여부 확인 (익명 사용자 제외)
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return true;
        }

        // 2. 요청 URI 확인 (무한 루프 방지 및 제외 경로 설정)
        String uri = request.getRequestURI();
        if (uri.startsWith("/member/set-username") || 
            uri.startsWith("/logout") || 
            uri.startsWith("/api/member/check-username") ||
            uri.startsWith("/css/") || 
            uri.startsWith("/js/") || 
            uri.startsWith("/image/") ||
            uri.equals("/error") ||
            uri.equals("/favicon.ico")) {
            return true;
        }

        // 3. DB에서 해당 사용자의 isUsernameSet 플래그 확인
        Optional<MemberEntity> memberOpt = memberRepository.findByUsername(auth.getName());
        if (memberOpt.isPresent()) {
            MemberEntity member = memberOpt.get();
            if (Boolean.FALSE.equals(member.getIsUsernameSet())) {
                // 아이디가 설정되지 않았으면 설정 페이지로 리다이렉트
                response.sendRedirect("/member/set-username");
                return false;
            }
        }

        return true;
    }
}
