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

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import com.passfail.member.dto.OAuth2Member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.security.Principal;

/**
 * 마이페이지 및 사용자 프로필 관련 요청을 처리하는 컨트롤러 클래스
 * 프로필 조회, 정보 수정, 비밀번호 변경, 계정 탈퇴 등의 기능을 담당합니다.
 */
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;
    private final MemberRepository memberRepository;

    /**
     * 현재 로그인한 사용자의 마이페이지로 리다이렉트
     */
    @GetMapping
    public String myPage(Principal principal) {
        if (principal == null) return "redirect:/login";
        
        // 소셜 로그인 계정의 경우 principal.getName()이 고유 ID일 수 있으므로 DB에서 실제 username(닉네임)을 조회
        String username = memberRepository.findByUsername(principal.getName())
                .map(com.passfail.entity.MemberEntity::getUsername)
                .orElse(principal.getName());
                
        // 한글 닉네임 등을 고려하여 URL 인코딩 처리
        String encodedUsername = java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
        return "redirect:/mypage/" + encodedUsername;
    }

    /**
     * 특정 사용자의 프로필 페이지 조회
     * 문제 풀이 내역, 제출 내역, 알림(본인인 경우) 등을 포함합니다.
     */
    @GetMapping("/{username}")
    public String userProfile(@PathVariable("username") String username, 
                              @RequestParam(value = "solvedPage", defaultValue = "0") int solvedPage,
                              @RequestParam(value = "subPage", defaultValue = "0") int subPage,
                              Principal principal, Model model) {
        String loggedInUsername = (principal != null) ? principal.getName() : null;
        
        try {
            // 회원 기본 정보 및 본인 여부 확인
            MemberInfoResponse memberResponse = mypageService.getMemberInfoResponse(username, loggedInUsername);
            model.addAttribute("member", memberResponse);
            
            // 활동 데이터 페이징 설정 (페이지당 5개)
            int pageSize = 5;
            PageRequest subPageable = PageRequest.of(subPage, pageSize, Sort.by("submittedAt").descending());
            PageRequest solvedPageable = PageRequest.of(solvedPage, pageSize, Sort.by("firstSolvedAt").descending());
            
            // 최근 제출 내역 및 해결한 문제 목록 조회
            Page<com.passfail.entity.SubmissionEntity> submissionPage = mypageService.getRecentSubmissions(username, subPageable);
            Page<com.passfail.entity.SolvedProblemEntity> solvedPageObj = mypageService.getSolvedProblems(username, solvedPageable);
            
            model.addAttribute("submissionPage", submissionPage);
            model.addAttribute("solvedPage", solvedPageObj);
            
            // 통계 정보 추가
            model.addAttribute("totalSolvedCount", solvedPageObj.getTotalElements());
            model.addAttribute("totalSubmissionCount", submissionPage.getTotalElements());
            
            // 본인 프로필 조회 시에만 읽지 않은 알림 목록 추가
            if (memberResponse.isOwnProfile()) {
                model.addAttribute("notifications", mypageService.getNotifications(username));
            }
            
            return "member/mypage";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/main?error=UserNotFound";
        }
    }

    /**
     * 비밀번호 변경 페이지로 이동
     */
    @GetMapping("/change-password")
    public String changePasswordPage(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        MemberInfoResponse member = mypageService.getMemberInfoResponse(principal.getName(), principal.getName());
        model.addAttribute("email", member.getEmail());
        return "member/change-password";
    }

    /**
     * 비밀번호 변경 처리
     */
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

    /**
     * 소셜 계정 연동 해제
     */
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

    /**
     * 회원 탈퇴 처리
     */
    @PostMapping("/withdraw")
    public String withdraw(Principal principal, jakarta.servlet.http.HttpServletRequest request) {
        if (principal == null) return "redirect:/login";
        
        try {
            mypageService.withdraw(principal.getName());
            // 세션 무효화 및 시큐리티 컨텍스트 초기화
            request.getSession().invalidate();
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            return "redirect:/login?withdrawn";
        } catch (Exception e) {
            String encodedMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/mypage?error=" + encodedMessage;
        }
    }
}
