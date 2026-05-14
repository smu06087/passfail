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

/**
 * 마이페이지 관련 데이터 조회 및 회원 정보 수정을 처리하는 서비스 클래스
 * 프로필 정보, 제출 이력, 알림 조회 및 계정 설정 변경 기능을 담당합니다.
 */
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

    /**
     * 회원 정보 응답 DTO 조회
     * @param username 조회할 사용자 닉네임
     * @param loggedInUsername 현재 로그인한 사용자 닉네임
     * @return 회원 정보 응답 DTO
     */
    public MemberInfoResponse getMemberInfoResponse(String username, String loggedInUsername) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));
        boolean isOwnProfile = username.equals(loggedInUsername);
        return MemberInfoResponse.from(member, isOwnProfile);
    }

    /**
     * 최근 제출 내역 리스트 조회
     */
    public List<SubmissionEntity> getRecentSubmissions(String username) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return submissionRepository.findByMemberOrderBySubmittedAtDesc(member);
    }

    /**
     * 최근 제출 내역 페이징 조회
     */
    public Page<SubmissionEntity> getRecentSubmissions(String username, Pageable pageable) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return submissionRepository.findByMember(member, pageable);
    }

    /**
     * 해결한 문제 리스트 조회
     */
    public List<SolvedProblemEntity> getSolvedProblems(String username) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return solvedProblemRepository.findByMember(member);
    }

    /**
     * 해결한 문제 페이징 조회
     */
    public Page<SolvedProblemEntity> getSolvedProblems(String username, Pageable pageable) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return solvedProblemRepository.findByMember(member, pageable);
    }

    /**
     * 사용자의 알림 목록 조회 (최신순)
     */
    public List<NotificationEntity> getNotifications(String username) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return notificationRepository.findByMemberOrderByCreatedAtDesc(member);
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(String username, String newPassword) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 비밀번호 강도 검증 (MemberService 활용)
        memberService.validatePassword(newPassword);

        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    /**
     * 소셜 계정 연동 해제 로직
     * 보조 연동 계정을 원래의 '베이스 멤버'로 되돌리는 방식으로 처리
     */
    @Transactional
    public void unlinkSocialAccount(String username, String providerName) {
        MemberEntity currentMember = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        Provider provider = Provider.valueOf(providerName.toUpperCase());
        SocialAccountEntity socialAccount = socialAccountRepository.findByMembersAndProvider(currentMember, provider)
                .orElseThrow(() -> new IllegalArgumentException("연동된 계정을 찾을 수 없습니다."));

        // 해당 소셜 계정의 고유 정보(Base Member) 조회
        SocialAccountEntity originalSocial = socialAccountRepository.findByProviderAndProviderId(provider, socialAccount.getProviderId())
                .orElseThrow(() -> new IllegalArgumentException("원본 계정 정보를 찾을 수 없어 해제할 수 없습니다."));

        // 현재 멤버가 베이스 멤버가 아닌 경우에만 해제 가능 (연동 이전 상태로 복구)
        if (!originalSocial.getMemberId().equals(currentMember.getMemberId())) {
            socialAccount.setMemberId(originalSocial.getMemberId());
            socialAccount.setMembers(originalSocial.getMembers());
            socialAccountRepository.saveAndFlush(socialAccount);
        } else {
            throw new IllegalArgumentException("기본 연동 계정은 해제할 수 없습니다.");
        }
    }

    /**
     * 회원 탈퇴 처리 (Soft Delete)
     * 계정을 비활성화하고 삭제 시각을 기록합니다.
     */
    @Transactional
    public void withdraw(String username) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        member.setIsActive(false);
        member.setDeletedAt(LocalDateTime.now());
        memberRepository.saveAndFlush(member);
    }
}
