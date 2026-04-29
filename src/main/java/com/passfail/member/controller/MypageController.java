package com.passfail.member.controller;

import com.passfail.member.dto.MemberInfoResponse;
import com.passfail.member.service.MypageService;
import com.passfail.member.repository.MemberRepository;
import com.passfail.entity.MemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.security.Principal;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;
    private final MemberRepository memberRepository;

    @GetMapping
    public String myPage(Principal principal) {
        if (principal == null) return "redirect:/login";
        
        // Find the actual username (nickname) from DB because principal.getName() 
        // might return a provider-specific unique ID for social accounts.
        String username = memberRepository.findByUsername(principal.getName())
                .map(com.passfail.entity.MemberEntity::getUsername)
                .orElse(principal.getName());
                
        // Encode for URL safety (Korean characters)
        String encodedUsername = java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
        return "redirect:/mypage/" + encodedUsername;
    }

    @GetMapping("/{username}")
    public String userProfile(@PathVariable("username") String username, 
                              @RequestParam(value = "solvedPage", defaultValue = "0") int solvedPage,
                              @RequestParam(value = "subPage", defaultValue = "0") int subPage,
                              Principal principal, Model model) {
        String loggedInUsername = (principal != null) ? principal.getName() : null;
        
        try {
            MemberInfoResponse memberResponse = mypageService.getMemberInfoResponse(username, loggedInUsername);
            model.addAttribute("member", memberResponse);
            
            // 페이지당 5개씩
            int pageSize = 5;
            
            // 활동 데이터 추가 (페이징 적용)
            PageRequest subPageable = PageRequest.of(subPage, pageSize, Sort.by("submittedAt").descending());
            PageRequest solvedPageable = PageRequest.of(solvedPage, pageSize, Sort.by("firstSolvedAt").descending());
            
            Page<com.passfail.entity.SubmissionEntity> submissionPage = mypageService.getRecentSubmissions(username, subPageable);
            Page<com.passfail.entity.SolvedProblemEntity> solvedPageObj = mypageService.getSolvedProblems(username, solvedPageable);
            
            model.addAttribute("submissionPage", submissionPage);
            model.addAttribute("solvedPage", solvedPageObj);
            
            // 통계용 총 개수
            model.addAttribute("totalSolvedCount", solvedPageObj.getTotalElements());
            model.addAttribute("totalSubmissionCount", submissionPage.getTotalElements());
            
            // 본인 프로필일 경우에만 알림 추가
            if (memberResponse.isOwnProfile()) {
                model.addAttribute("notifications", mypageService.getNotifications(username));
            }
            
            return "member/mypage";
        } catch (Exception e) {
            e.printStackTrace();
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
            String encodedNickname = java.net.URLEncoder.encode(nickname, java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/mypage/" + encodedNickname + "?updateSuccess";
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
