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
