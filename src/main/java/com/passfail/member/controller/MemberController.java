package com.passfail.member.controller;

import com.passfail.entity.MemberEntity;
import com.passfail.member.dto.MemberJoinRequest;
import com.passfail.member.repository.MemberRepository;
import com.passfail.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/*
  회원 관련 요청을 처리하는 컨트롤러 클래스
  로그인, 회원가입, 계정 복구, 이메일 인증 등 일반적인 사용자 관리 기능을 담당합니다.
*/
@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final com.passfail.member.service.MypageService mypageService;

    // 로그인 페이지로 이동

    @GetMapping("/login")
    public String loginPage() {
        return "member/login";
    }

    /*
      메인 페이지로 이동
      로그인 상태일 경우 비활성 계정 여부를 확인하고, 사용자 정보를 모델에 추가합니다.
    */
    @GetMapping({"/", "/main"})
    public String mainPage(Authentication authentication, Model model) {
        if (authentication != null) {
            // 비활성 계정 체크 (null-safe)
            MemberEntity member = memberRepository.findByUsername(authentication.getName()).orElse(null);
            if (member != null && Boolean.FALSE.equals(member.getIsActive())) {
                // 계정이 비활성화 상태이면 복구 페이지로 리다이렉트
                return "redirect:/recovery";
            }

            try {
                // 세션의 식별자(username)로 항상 일관된 정보 조회
                String username = mypageService.getMemberInfoResponse(authentication.getName(), authentication.getName()).getUsername();
                model.addAttribute("username", username);
            } catch (Exception e) {
                model.addAttribute("username", authentication.getName());
            }
        }
        return "main";
    }


    // 계정 복구 페이지로 이동

    @GetMapping("/recovery")
    public String recoveryPage(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";
        
        MemberEntity member = memberRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        if (member.getIsActive()) return "redirect:/main";
        
        model.addAttribute("username", member.getUsername());
        return "member/recovery";
    }
    
    // 비활성화된 계정을 다시 활성화
    @PostMapping("/recovery")
    public String recoverAccount(Authentication authentication) {
        if (authentication == null) return "redirect:/login";
        
        memberService.reactivate(authentication.getName());
        return "redirect:/main?recovered";
    }

    // 회원가입 페이지로 이동
    @GetMapping("/signup")
    public String signupPage() {
        return "member/signup";
    }

    
    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(@ModelAttribute MemberJoinRequest request) {
        try {
            memberService.register(request);
            return "redirect:/login?signupSuccess";
        } catch (Exception e) {
            String encodedMessage = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/signup?error=" + encodedMessage;
        }
    }

    // 아이디 중복 확인 API
    @GetMapping("/api/member/check-username")
    @ResponseBody
    public Map<String, Boolean> checkUsername(@RequestParam("username") String username) {
        return Map.of("available", memberService.isUsernameAvailable(username));
    }

    // 이메일 인증 코드 발송 API
    @PostMapping("/api/member/send-verification")
    @ResponseBody
    public Map<String, String> sendVerification(@RequestParam("email") String email) {
        try {
            memberService.sendVerificationEmail(email);
            return Map.of("status", "success", "message", "인증 코드가 발송되었습니다.");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    
    // 이메일 인증 코드 확인 API
    
    @PostMapping("/api/member/verify-email")
    @ResponseBody
    public Map<String, Object> verifyEmail(@RequestParam("email") String email, @RequestParam("code") String code) {
        try {
            boolean verified = memberService.verifyEmail(email, code);
            return Map.of("status", verified ? "success" : "error", "verified", verified);
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    
    // 비밀번호 찾기 페이지로 이동
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "member/forgot-password";
    }

    // 비밀번호 재설정 처리 (임시 비밀번호 발송)
    @PostMapping("/forgot-password")
    public String resetPassword(@RequestParam("email") String email) {
        try {
            memberService.resetPassword(email);
            return "redirect:/login?resetSuccess";
        } catch (Exception e) {
            return "redirect:/forgot-password?error";
        }
    }

    /**
     * 아이디 설정 페이지 이동
     */
    @GetMapping("/member/set-username")
    public String setUsernamePage(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";
        
        MemberEntity member = memberRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        // 이미 설정했다면 메인으로
        if (Boolean.TRUE.equals(member.getIsUsernameSet())) return "redirect:/main";
        
        model.addAttribute("currentUsername", member.getUsername());
        return "member/set-username";
    }

    /**
     * 아이디 설정 처리
     */
    @PostMapping("/member/set-username")
    public String setUsername(Authentication authentication, @RequestParam("username") String newUsername) {
        if (authentication == null) return "redirect:/login";

        try {
            // 1. 형식 및 중복 검증
            if (!newUsername.matches("^[a-zA-Z0-9]{7,}$")) {
                throw new IllegalArgumentException("아이디는 영문과 숫자 조합으로 7자 이상이어야 합니다.");
            }
            if (!memberService.isUsernameAvailable(newUsername)) {
                throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
            }

            // 2. DB 업데이트
            MemberEntity member = memberRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
            
            member.setUsername(newUsername);
            member.setIsUsernameSet(true);
            memberRepository.saveAndFlush(member);

            // 3. 시큐리티 컨텍스트 갱신 (중요: principal의 name을 변경된 username으로 교체)
            refreshSecurityContext(authentication, member);

            return "redirect:/main?welcome";
        } catch (Exception e) {
            String encodedMessage = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/member/set-username?error=" + encodedMessage;
        }
    }

    /**
     * 세션의 인증 정보를 새 아이디로 갱신하는 헬퍼 메서드
     */
    private void refreshSecurityContext(Authentication auth, MemberEntity member) {
        Authentication newAuth = null;

        if (auth instanceof OAuth2AuthenticationToken token) {
            // 소셜 로그인의 경우 커스텀 OAuth2Member를 다시 생성하여 교체
            com.passfail.member.dto.OAuth2Member oldPrincipal = (com.passfail.member.dto.OAuth2Member) token.getPrincipal();
            com.passfail.member.dto.OAuth2Member newPrincipal = new com.passfail.member.dto.OAuth2Member(
                oldPrincipal.getOriginalUser(),
                member.getUsername(),
                member.getRole()
            );
            
            newAuth = new OAuth2AuthenticationToken(
                newPrincipal,
                newPrincipal.getAuthorities(),
                token.getAuthorizedClientRegistrationId()
            );
        } else if (auth instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token) {
            // 로컬 로그인의 경우 (일반적으로는 이미 설정되어 들어오지만 예외 처리)
            org.springframework.security.core.userdetails.User newUserDetails = (org.springframework.security.core.userdetails.User) org.springframework.security.core.userdetails.User.builder()
                .username(member.getUsername())
                .password("") // 비밀번호는 세션 갱신 시 불필요하거나 기존 값 유지
                .authorities(token.getAuthorities())
                .build();
            
            newAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                newUserDetails,
                token.getCredentials(),
                token.getAuthorities()
            );
        }

        if (newAuth != null) {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);
        }
    }

    // OAuth2 제공자별 고유 ID 추출 (내부 헬퍼 메서드)
    private String getProviderId(String provider, OAuth2User oauth2User) {
        if ("kakao".equals(provider)) return oauth2User.getAttribute("id").toString();
        if ("google".equals(provider)) return oauth2User.getAttribute("sub");
        if ("naver".equals(provider)) {
            Map<String, Object> response = (Map<String, Object>) oauth2User.getAttribute("response");
            return (String) response.get("id");
        }
        if ("github".equals(provider)) return oauth2User.getAttribute("id").toString();
        return "";
    }
}
