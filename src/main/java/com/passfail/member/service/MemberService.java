package com.passfail.member.service;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.EmailVerificationEntity;
import com.passfail.entity.TermsConsentEntity;
import com.passfail.enums.Role;
import com.passfail.member.dto.MemberJoinRequest;
import com.passfail.member.repository.MemberRepository;
import com.passfail.member.repository.SocialAccountRepository;
import com.passfail.member.repository.EmailVerificationRepository;
import com.passfail.member.repository.TermsConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * 회원 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 회원가입, 인증, 비밀번호 관리 및 Spring Security의 UserDetailsService 구현을 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final TermsConsentRepository termsConsentRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    /**
     * Spring Security에서 사용자 정보를 로드하는 메서드
     * @param username 사용자 식별자 (ID)
     * @return UserDetails 객체
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        return new User(
                member.getUsername(),
                member.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(member.getRole().name()))
        );
    }

    /**
     * 아이디 사용 가능 여부 확인
     * @param username 확인할 아이디
     * @return 사용 가능 여부 (7자 이상이고 중복되지 않아야 함)
     */
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.length() < 7) {
            return false;
        }
        return memberRepository.findByUsername(username).isEmpty();
    }

    /**
     * 비밀번호 규칙 검증
     * @param password 검증할 비밀번호
     */
    public void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8글자 이상이어야 합니다.");
        }
        // 최소 1개의 특수문자 포함 여부 체크
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new IllegalArgumentException("비밀번호는 최소 1개의 특수문자를 포함해야 합니다.");
        }
    }
    
    /**
     * Username을 가지고 MemberId를 얻어옵니다.
     * @param username 확인할 아이디
     * @return ID in the members table. -1L if there is no member data.
     */
    public Long getMemberIdByUsername(String username) {
    	Optional<MemberEntity> member = memberRepository.findByUsername(username);                
    	 
    	if(member.isPresent())
    	{    	 
    		return member.get().getMemberId();
    	}    	
    	return -1L;
    }

    /**
     * 이메일 인증 코드 발송
     * @param email 수신 이메일 주소
     */
    @Transactional
    public void sendVerificationEmail(String email) {
        // 6자리 대문자 랜덤 코드 생성
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        EmailVerificationEntity verification = EmailVerificationEntity.builder()
                .email(email)
                .verificationCode(code)
                .expiryDate(LocalDateTime.now().plusMinutes(5)) // 5분 만료
                .isVerified(false)
                .build();
        
        emailVerificationRepository.save(verification);

        // 비동기 메일 발송
        mailService.sendMail(email, "[Passfail] 이메일 인증 코드", "인증 코드는 [" + code + "] 입니다. 5분 이내에 입력해 주세요.");
    }

    /**
     * 이메일 인증 코드 확인
     * @param email 인증할 이메일
     * @param code 입력한 인증 코드
     * @return 인증 성공 여부
     */
    @Transactional
    public boolean verifyEmail(String email, String code) {
        EmailVerificationEntity verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청 기록이 없습니다."));

        if (verification.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다.");
        }

        if (verification.getVerificationCode().equals(code)) {
            verification.setIsVerified(true);
            return true;
        }
        return false;
    }

    /**
     * 회원 가입 및 계정 통합 처리
     * @param request 가입 요청 DTO
     */
    @Transactional
    public void register(MemberJoinRequest request) {
        // 1. 아이디 형식 및 중복 확인
        if (request.getUsername() == null || !request.getUsername().matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("아이디는 영문과 숫자만 사용 가능합니다.");
        }

        Optional<MemberEntity> existingByUsername = memberRepository.findByUsername(request.getUsername());
        if (existingByUsername.isPresent()) {
            if (!existingByUsername.get().getEmail().equals(request.getEmail())) {
                throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
            }
        }
        
        // 2. 비밀번호 규칙 검증
        validatePassword(request.getPassword());

        // 3. 이메일 인증 확인
        EmailVerificationEntity verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 인증이 필요합니다."));

        if (!verification.getIsVerified()) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        if (request.getPrivacyPolicyAgreed() == null || !request.getPrivacyPolicyAgreed()) {
            throw new IllegalArgumentException("개인정보 수집 및 이용에 동의해야 합니다.");
        }

        // 4. 이메일로 기존 회원 조회 (소셜 계정 여부 확인 및 통합)
        Optional<MemberEntity> existingMember = memberRepository.findByEmail(request.getEmail());
        MemberEntity member;

        if (existingMember.isPresent()) {
            member = existingMember.get();
            // 이미 로컬 비밀번호가 설정되어 있다면 중복 가입 방지
            if (member.getPassword() != null && !member.getPassword().isEmpty()) {
                throw new IllegalArgumentException("이미 해당 이메일로 가입된 로컬 계정이 존재합니다. 로그인을 시도해 주세요.");
            }
            // 기존 소셜 계정에 아이디와 비밀번호를 추가하여 통합
            member.setUsername(request.getUsername());
            member.setPassword(passwordEncoder.encode(request.getPassword()));
            member.setIsActive(true);
        } else {
            // 신규 회원인 경우 새로 생성
            member = MemberEntity.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.ROLE_USER)
                    .isActive(true)
                    .pointBalance(0)
                    .totalScore(0)
                    .build();
        }

        MemberEntity savedMember = memberRepository.save(member);

        // 5. 약관 동의 저장 (통합 시에도 동의 내역 저장/갱신)
        TermsConsentEntity consent = termsConsentRepository.findByMember(savedMember)
                .orElse(new TermsConsentEntity());
        
        consent.setMember(savedMember);
        consent.setPrivacyPolicyAgreed(request.getPrivacyPolicyAgreed());
        termsConsentRepository.save(consent);
    }

    /**
     * 비밀번호 재설정 (임시 비밀번호 발급)
     * @param email 비밀번호를 재설정할 이메일
     */
    @Transactional
    public void resetPassword(String email) {
        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일로 가입된 사용자를 찾을 수 없습니다."));

        // 1. 임시 비밀번호 생성 (8자리)
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        
        // 2. 비밀번호 암호화 및 저장
        member.setPassword(passwordEncoder.encode(tempPassword));
        memberRepository.save(member);

        // 3. 비동기 메일 발송
        mailService.sendMail(email, "[Passfail] 임시 비밀번호 안내", 
                "안녕하세요. 요청하신 임시 비밀번호는 [" + tempPassword + "] 입니다.\n" +
                "로그인 후 반드시 비밀번호를 변경해 주세요.");
    }

    /**
     * 탈퇴한 계정 재활성화
     * @param username 재활성화할 사용자 ID
     */
    @Transactional
    public void reactivate(String username) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        member.setIsActive(true);
        member.setDeletedAt(null); // 복구 시 탈퇴 시간 초기화
        memberRepository.saveAndFlush(member);
    }
}
