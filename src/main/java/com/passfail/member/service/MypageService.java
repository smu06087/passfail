package com.passfail.member.service;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.NotificationEntity;
import com.passfail.entity.SocialAccountEntity;
import com.passfail.entity.SolvedProblemEntity;
import com.passfail.entity.SubmissionEntity;
import com.passfail.enums.Provider;
import com.passfail.member.dto.MemberInfoResponse;
import com.passfail.member.repository.MemberRepository;
import com.passfail.member.repository.NotificationRepository;
import com.passfail.member.repository.SocialAccountRepository;
import com.passfail.problem.repository.SolvedProblemRepository;
import com.passfail.problem.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MypageService {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberService memberService;
    private final SubmissionRepository submissionRepository;
    private final SolvedProblemRepository solvedProblemRepository;
    private final NotificationRepository notificationRepository;

    public MemberInfoResponse getMemberInfoResponse(String username, String loggedInUsername) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));
        boolean isOwnProfile = username.equals(loggedInUsername);
        return MemberInfoResponse.from(member, isOwnProfile);
    }

    public List<SubmissionEntity> getRecentSubmissions(String username) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return submissionRepository.findByMemberOrderBySubmittedAtDesc(member);
    }

    public Page<SubmissionEntity> getRecentSubmissions(String username, Pageable pageable) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return submissionRepository.findByMember(member, pageable);
    }

    public List<SolvedProblemEntity> getSolvedProblems(String username) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return solvedProblemRepository.findByMember(member);
    }

    public Page<SolvedProblemEntity> getSolvedProblems(String username, Pageable pageable) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return solvedProblemRepository.findByMember(member, pageable);
    }

    public List<NotificationEntity> getNotifications(String username) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return notificationRepository.findByMemberOrderByCreatedAtDesc(member);
    }

    @Transactional
    public void updateNickname(String currentUsername, String newNickname) {
        MemberEntity member = memberRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        if (memberRepository.findByUsername(newNickname).isPresent() && !currentUsername.equals(newNickname)) {
            throw new IllegalArgumentException("이미 사용 중인 이름입니다.");
        }
        
        member.setUsername(newNickname);
    }

    @Transactional
    public void changePassword(String username, String newPassword) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        memberService.validatePassword(newPassword);

        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    @Transactional
    public void unlinkSocialAccount(String username, String providerName) {
        MemberEntity currentMember = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        Provider provider = Provider.valueOf(providerName.toUpperCase());
        SocialAccountEntity socialAccount = socialAccountRepository.findByMembersAndProvider(currentMember, provider)
                .orElseThrow(() -> new IllegalArgumentException("연동된 계정을 찾을 수 없습니다."));

        // 베이스 멤버(해당 소셜로 처음 가입된 멤버) 확인
        SocialAccountEntity originalSocial = socialAccountRepository.findByProviderAndProviderId(provider, socialAccount.getProviderId())
                .orElseThrow(() -> new IllegalArgumentException("원본 계정 정보를 찾을 수 없어 해제할 수 없습니다."));

        if (!originalSocial.getMemberId().equals(currentMember.getMemberId())) {
            socialAccount.setMemberId(originalSocial.getMemberId());
            socialAccount.setMembers(originalSocial.getMembers());
            socialAccountRepository.saveAndFlush(socialAccount);
        } else {
            throw new IllegalArgumentException("기본 연동 계정은 해제할 수 없습니다.");
        }
    }

    @Transactional
    public void withdraw(String username) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        member.setIsActive(false);
        member.setDeletedAt(LocalDateTime.now());
        memberRepository.saveAndFlush(member);
    }
}
