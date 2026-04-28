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

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final com.passfail.member.service.MypageService mypageService;

    @GetMapping("/login")
    public String loginPage() {
        return "member/login";
    }

    @GetMapping({"/", "/main"})
    public String mainPage(Authentication authentication, Model model) {
        if (authentication != null) {
            // 비활성 계정 체크 (null-safe)
            MemberEntity member = memberRepository.findByUsername(authentication.getName()).orElse(null);
            if (member != null && Boolean.FALSE.equals(member.getIsActive())) {
                return "redirect:/recovery";
            }

            try {
                // 세션의 식별자(username)로 항상 일관된 정보 조회 (2개 인자 전달)
                String username = mypageService.getMemberInfoResponse(authentication.getName(), authentication.getName()).getUsername();
                model.addAttribute("username", username);
            } catch (Exception e) {
                model.addAttribute("username", authentication.getName());
            }
        }
        return "main";
    }

    @GetMapping("/recovery")
    public String recoveryPage(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";
        
        MemberEntity member = memberRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        if (member.getIsActive()) return "redirect:/main";
        
        model.addAttribute("username", member.getUsername());
        return "member/recovery";
    }

    @PostMapping("/recovery")
    public String recoverAccount(Authentication authentication) {
        if (authentication == null) return "redirect:/login";
        
        memberService.reactivate(authentication.getName());
        return "redirect:/main?recovered";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "member/signup";
    }

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

    @GetMapping("/api/member/check-username")
    @ResponseBody
    public Map<String, Boolean> checkUsername(@RequestParam("username") String username) {
        return Map.of("available", memberService.isUsernameAvailable(username));
    }

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

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "member/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String resetPassword(@RequestParam("email") String email) {
        try {
            memberService.resetPassword(email);
            return "redirect:/login?resetSuccess";
        } catch (Exception e) {
            return "redirect:/forgot-password?error";
        }
    }

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
