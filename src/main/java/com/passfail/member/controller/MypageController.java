package com.passfail.member.controller;

import com.passfail.member.dto.MemberInfoResponse;
import com.passfail.member.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;

    @GetMapping
    public String myPage(Principal principal) {
        if (principal == null) return "redirect:/login";
        return "redirect:/mypage/" + principal.getName();
    }

    @GetMapping("/{username}")
    public String userProfile(@PathVariable("username") String username, Principal principal, Model model) {
        String loggedInUsername = (principal != null) ? principal.getName() : null;
        
        try {
            MemberInfoResponse memberResponse = mypageService.getMemberInfoResponse(username, loggedInUsername);
            model.addAttribute("member", memberResponse);
            
            // 활동 데이터 추가
            model.addAttribute("recentSubmissions", mypageService.getRecentSubmissions(username));
            model.addAttribute("solvedProblems", mypageService.getSolvedProblems(username));
            
            // 본인 프로필일 경우에만 알림 추가
            if (memberResponse.isOwnProfile()) {
                model.addAttribute("notifications", mypageService.getNotifications(username));
            }
            
            return "member/mypage";
        } catch (Exception e) {
            return "redirect:/main?error=UserNotFound";
        }
    }

    @GetMapping("/change-password")
    public String changePasswordPage(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        MemberInfoResponse member = mypageService.getMemberInfoResponse(principal.getName(), principal.getName());
        model.addAttribute("email", member.getEmail());
        return "member/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(Principal principal, @RequestParam("newPassword") String newPassword) {
        if (principal == null) return "redirect:/login";
        try {
            mypageService.changePassword(principal.getName(), newPassword);
            return "redirect:/mypage?passwordSuccess";
        } catch (Exception e) {
            String encodedMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/mypage/change-password?error=" + encodedMessage;
        }
    }

    @PostMapping("/update")
    public String updateInfo(Principal principal, @RequestParam("nickname") String nickname) {
        if (principal == null) return "redirect:/login";
        
        try {
            mypageService.updateNickname(principal.getName(), nickname);
            return "redirect:/mypage/" + nickname + "?updateSuccess";
        } catch (Exception e) {
            String encodedMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/mypage?error=" + encodedMessage;
        }
    }

    @PostMapping("/unlink")
    public String unlinkSocial(@RequestParam("provider") String provider, Principal principal) {
        if (principal == null) return "redirect:/login";
        
        try {
            mypageService.unlinkSocialAccount(principal.getName(), provider);
            return "redirect:/mypage?unlinkSuccess";
        } catch (Exception e) {
            String encodedMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/mypage?error=" + encodedMessage;
        }
    }

    @PostMapping("/withdraw")
    public String withdraw(Principal principal, jakarta.servlet.http.HttpServletRequest request) {
        if (principal == null) return "redirect:/login";
        
        try {
            mypageService.withdraw(principal.getName());
            request.getSession().invalidate();
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            return "redirect:/login?withdrawn";
        } catch (Exception e) {
            String encodedMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/mypage?error=" + encodedMessage;
        }
    }
}
