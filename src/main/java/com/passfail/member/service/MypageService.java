package com.passfail.member.service;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.NotificationEntity;
import com.passfail.entity.SocialAccountEntity;
import com.passfail.entity.SolvedProblemEntity;
import com.passfail.entity.SubmissionEntity;
import com.passfail.enums.Provider;
import com.passfail.enums.Role;
import com.passfail.member.dto.MemberInfoResponse;
import com.passfail.member.repository.MemberRepository;
import com.passfail.member.repository.SocialAccountRepository;
import com.passfail.problem.repository.SolvedProblemRepository;
import com.passfail.problem.repository.SubmissionRepository;
import com.passfail.post.repository.CommentRepository;
import com.passfail.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.passfail.entity.PostEntity;
import com.passfail.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 마이페이지 관련 데이터 조회 및 회원 정보 수정을 처리하는 서비스 클래스
 * 프로필 정보, 제출 이력, 게시글/댓글 내역 조회 및 계정 설정 변경 기능을 담당합니다.
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
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final com.passfail.ranking.repository.TotalTierRepository totalTierRepository;

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
        MemberInfoResponse response = MemberInfoResponse.from(member, isOwnProfile);
        
        // 실시간 랭킹 정보 조회 (TotalTierEntity에서 최신 데이터 가져오기)
        totalTierRepository.findByMember_MemberId(member.getMemberId()).ifPresent(tier -> {
            response.setTotalScore(tier.getTotalScore());
            response.setGlobalRank(tier.getCurrentRank());
            response.setTier(tier.getTier());
        });
        
        return response;
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
     * 해결한 문제 페이징 조회
     */
    public Page<SolvedProblemEntity> getSolvedProblems(String username, Pageable pageable) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return solvedProblemRepository.findByMember(member, pageable);
    }

    /**
     * 내가 쓴 게시글 페이징 조회
     */
    public Page<PostEntity> getMyPosts(String username, Pageable pageable) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return postRepository.findByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(member.getMemberId(), pageable);
    }

    /**
     * 좋아요 한 게시글 페이징 조회
     */
    public Page<PostEntity> getLikedPosts(String username, Pageable pageable) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return postRepository.findLikedPostsByMemberId(member.getMemberId(), pageable);
    }

    /**
     * 내가 쓴 댓글 페이징 조회
     */
    public Page<CommentEntity> getMyComments(String username, Pageable pageable) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return commentRepository.findByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(member.getMemberId(), pageable);
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
     * 연동된 소셜 계정을 기존의 베이스 멤버로 되돌리거나, 
     * 이미 베이스 멤버인 경우 새로운 독립 계정을 생성하여 분리합니다.
     */
    @Transactional
    public void unlinkSocialAccount(String username, String providerName) {
        MemberEntity currentMember = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        Provider provider = Provider.valueOf(providerName.toUpperCase());
        SocialAccountEntity socialAccount = socialAccountRepository.findByMembersAndProvider(currentMember, provider)
                .orElseThrow(() -> new IllegalArgumentException("연동된 계정을 찾을 수 없습니다."));

        // 베이스 멤버 이메일 형식 구성 (CustomOAuth2UserService의 로직과 일치)
        String baseEmail = provider.name().toLowerCase() + "_" + socialAccount.getProviderId() + "@passfail.com";
        
        // 1. 기존 베이스 멤버(이 소셜 계정 고유의 메일 주소를 가진 멤버)가 있는지 확인
        Optional<MemberEntity> baseMemberOpt = memberRepository.findByEmail(baseEmail);
        
        MemberEntity targetMember;
        
        if (baseMemberOpt.isPresent() && !baseMemberOpt.get().getMemberId().equals(currentMember.getMemberId())) {
            // 현재 멤버가 아닌 다른 베이스 멤버가 존재하면 그쪽으로 연동 정보를 되돌림 (기존 번호로 수정)
            targetMember = baseMemberOpt.get();
        } else {
            // 베이스 멤버가 없거나, 현재 멤버가 이미 베이스 멤버인 경우
            // 연동 해제 시 완전히 새로운 계정을 생성하여 소셜 정보를 격리함 (새 번호 생성)
            String nickname = provider.name() + "_" + socialAccount.getProviderId().substring(0, Math.min(socialAccount.getProviderId().length(), 5));
            // 이메일 중복 방지를 위해 고유한 이메일 생성
            String uniqueEmail = provider.name().toLowerCase() + "_" + System.currentTimeMillis() + "_" + socialAccount.getProviderId() + "@passfail.com";
            targetMember = createNewBaseMember(nickname, uniqueEmail, currentMember.getProfileImage());
        }

        // social_account 테이블의 member_id를 타겟 멤버로 변경하여 연동 해제 처리
        socialAccount.setMemberId(targetMember.getMemberId());
        // Lazy Loading 방지 및 영속성 컨텍스트 동기화를 위해 members 객체도 설정
        socialAccount.setMembers(targetMember);
        
        socialAccountRepository.saveAndFlush(socialAccount);
    }

    /**
     * 연동 해제용 신규 독립 멤버 생성
     */
    private MemberEntity createNewBaseMember(String nickname, String email, String profileImage) {
        String uniqueUsername = nickname;
        int count = 0;
        // 닉네임 중복 방지
        while (memberRepository.findByUsername(uniqueUsername).isPresent()) {
            count++;
            uniqueUsername = nickname + "_" + count;
        }
        
        MemberEntity newMember = MemberEntity.builder()
                .username(uniqueUsername)
                .email(email)
                .profileImage(profileImage)
                .role(Role.ROLE_USER)
                .isActive(true)
                .isSocial(true)
                .isUsernameSet(true)
                .pointBalance(0)
                .totalScore(0)
                .build();
        
        return memberRepository.saveAndFlush(newMember);
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
