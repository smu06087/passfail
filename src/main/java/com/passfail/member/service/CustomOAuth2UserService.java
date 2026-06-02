package com.passfail.member.service;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.SocialAccountEntity;
import com.passfail.enums.Provider;
import com.passfail.enums.Role;
import com.passfail.member.repository.MemberRepository;
import com.passfail.member.repository.SocialAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

import com.passfail.member.dto.OAuth2Member;

/**
 * OAuth2 소셜 로그인을 처리하는 서비스 클래스
 * 카카오, 구글, 네이버, 깃허브 등 외부 서비스의 사용자 정보를 기반으로 로그인 및 회원 연동을 담당합니다.
 */
@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    /**
     * 소셜 서비스로부터 사용자 정보를 가져온 후 후속 처리를 수행
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception e) {
            log.error("OAuth2 Login Error: {}", e.getMessage());
            throw new OAuth2AuthenticationException(e.getMessage());
        }
    }

    /**
     * 소셜 사용자 정보 처리 핵심 로직
     * 1. 소셜 데이터 추출
     * 2. 기존 연동 여부 확인
     * 3. 자동 계정 통합 또는 신규 가입 처리
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Provider provider = Provider.valueOf(registrationId.toUpperCase());
        
        // 1. 소셜 데이터 추출 (제공자별 상이한 구조 처리)
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String providerId = getProviderId(registrationId, attributes);
        String nickname = getNickname(registrationId, attributes);
        String email = getEmail(registrationId, attributes);
        String profileImage = getProfileImage(registrationId, attributes);

        // 닉네임이 없는 경우 기본값 설정
        if (nickname == null || nickname.isEmpty()) nickname = registrationId + "_" + providerId;
        
        final String nicknameForLambda = nickname;
        final String emailForLambda = email;
        final String profileImageForLambda = profileImage;

        // 2. 현재 로그인된 유저(연동 주체) 확인 (마이페이지에서 추가 연동하는 경우)
        MemberEntity currentMember = getCurrentLoggedInMember();

        // 3. 소셜 정보로 기존 연동 이력 조회
        Optional<SocialAccountEntity> socialOpt = socialAccountRepository.findByProviderAndProviderId(provider, providerId);
        
        MemberEntity finalMember;

        if (currentMember != null) {
            // [연동 모드] 이미 로그인된 상태에서 새로운 소셜 계정 연결 시도
            finalMember = currentMember;
            
            if (socialOpt.isPresent()) {
                SocialAccountEntity existingSocial = socialOpt.get();
                // 이미 연동된 정보가 다른 유저의 것이라면 현재 유저로 소유권 이전
                if (!existingSocial.getMemberId().equals(currentMember.getMemberId())) {
                    log.info("Social account {} ownership transferred to {}", provider, currentMember.getMemberId());
                    existingSocial.setMemberId(currentMember.getMemberId());
                    existingSocial.setMembers(currentMember);
                    socialAccountRepository.save(existingSocial);
                }
            } else {
                // 처음 연동하는 소셜 계정: 해제 시 돌아갈 "베이스 멤버" 생성 후 현재 유저와 연동
                ensureBaseMemberExists(nicknameForLambda, profileImageForLambda, provider, providerId);
                saveSocialAccount(currentMember, provider, providerId);
            }
        } else {
            // [로그인/가입 모드] 로그인하지 않은 상태에서 소셜 로그인 시도
            if (socialOpt.isPresent()) {
                // 이미 연동된 정보가 있으면 해당 멤버로 로그인
                finalMember = socialOpt.get().getMembers();
            } else {
                // 신규 소셜 접근
                // 1. members 테이블에 이 소셜 계정 고유의 레코드(Base Member)를 먼저 생성 (연동 해제 시 자생을 위함)
                String baseEmail = provider.name().toLowerCase() + "_" + providerId + "@passfail.com";
                MemberEntity baseMember = createNewMember(nicknameForLambda, baseEmail, profileImageForLambda);
                
                // 2. 소셜 계정의 실제 이메일과 동일한 로컬 계정이 있는지 확인 (자동 통합)
                String actualEmail = (emailForLambda != null) ? emailForLambda : baseEmail;
                Optional<MemberEntity> existingMemberOpt = memberRepository.findByEmail(actualEmail);
                
                if (existingMemberOpt.isPresent() && !existingMemberOpt.get().getMemberId().equals(baseMember.getMemberId())) {
                    // 동일 이메일 계정이 존재하면 자동으로 해당 계정에 연동
                    finalMember = existingMemberOpt.get();
                    log.info("Automatic linking: Social account {} linked to existing member {}", provider, finalMember.getEmail());
                } else {
                    // 동일 이메일이 없으면 방금 만든 베이스 멤버를 실제 멤버로 사용
                    if (emailForLambda != null) {
                        baseMember.setEmail(emailForLambda);
                        memberRepository.save(baseMember);
                    }
                    finalMember = baseMember;
                }
                // social_account 테이블에 최종 연동 정보 저장
                saveSocialAccount(finalMember, provider, providerId);
            }
        }

        // 공통: 프로필 이미지가 없는 경우에만 소셜 프로필로 설정
        if (profileImage != null && (finalMember.getProfileImage() == null || finalMember.getProfileImage().isEmpty())) {
            finalMember.setProfileImage(profileImage);
            memberRepository.save(finalMember);
        }

        // 사용자 닉네임과 DB 권한을 포함한 커스텀 OAuth2User 반환
        return new OAuth2Member(oAuth2User, finalMember.getUsername(), finalMember.getRole());
    }

    /**
     * 연동 해제를 대비한 소셜 계정 고유의 '베이스 멤버' 존재 여부 확인 및 생성
     */
    private void ensureBaseMemberExists(String nickname, String profileImage, Provider provider, String providerId) {
        if (socialAccountRepository.findByProviderAndProviderId(provider, providerId).isEmpty()) {
            String baseEmail = provider.name().toLowerCase() + "_" + providerId + "@passfail.com";
            createNewMember(nickname, baseEmail, profileImage);
        }
    }

    /**
     * 현재 시큐리티 컨텍스트에서 로그인된 사용자 정보를 가져옴
     */
    private MemberEntity getCurrentLoggedInMember() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || (auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            return null;
        }
        return memberRepository.findByUsername(auth.getName()).orElse(null);
    }

    /**
     * 서비스 제공자별 고유 ID 추출
     */
    private String getProviderId(String regId, Map<String, Object> attr) {
        if ("kakao".equals(regId)) return attr.get("id").toString();
        if ("google".equals(regId)) return (String) attr.get("sub");
        if ("naver".equals(regId)) return (String) ((Map) attr.get("response")).get("id");
        if ("github".equals(regId)) return attr.get("id").toString();
        return "";
    }

    /**
     * 서비스 제공자별 닉네임 추출
     */
    private String getNickname(String regId, Map<String, Object> attr) {
        if ("kakao".equals(regId)) {
            Map properties = (Map) attr.get("properties");
            return properties != null ? (String) properties.get("nickname") : null;
        }
        if ("google".equals(regId)) return (String) attr.get("name");
        if ("naver".equals(regId)) return (String) ((Map) attr.get("response")).get("nickname");
        if ("github".equals(regId)) return (String) attr.get("login");
        return null;
    }

    /**
     * 서비스 제공자별 이메일 추출
     */
    private String getEmail(String regId, Map<String, Object> attr) {
        if ("kakao".equals(regId)) {
            Map account = (Map) attr.get("kakao_account");
            return account != null ? (String) account.get("email") : null;
        }
        if ("google".equals(regId)) return (String) attr.get("email");
        if ("naver".equals(regId)) return (String) ((Map) attr.get("response")).get("email");
        if ("github".equals(regId)) return (String) attr.get("email");
        return null;
    }

    /**
     * 서비스 제공자별 프로필 이미지 URL 추출
     */
    private String getProfileImage(String regId, Map<String, Object> attr) {
        if ("kakao".equals(regId)) {
            Map properties = (Map) attr.get("properties");
            return properties != null ? (String) properties.get("profile_image") : null;
        }
        if ("google".equals(regId)) return (String) attr.get("picture");
        if ("naver".equals(regId)) return (String) ((Map) attr.get("response")).get("profile_image");
        if ("github".equals(regId)) return (String) attr.get("avatar_url");
        return null;
    }

    /**
     * 소셜 연동 정보 저장
     */
    private void saveSocialAccount(MemberEntity member, Provider provider, String providerId) {
        SocialAccountEntity socialAccount = SocialAccountEntity.builder()
                .memberId(member.getMemberId())
                .members(member)
                .provider(provider)
                .providerId(providerId)
                .build();
        socialAccountRepository.saveAndFlush(socialAccount);
    }

    /**
     * 신규 회원(Base Member) 생성 (닉네임 중복 시 접미사 추가)
     */
    private MemberEntity createNewMember(String nickname, String email, String profileImage) {
        String uniqueUsername = nickname;
        int count = 0;
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
                .isUsernameSet(false)
                .pointBalance(0)
                .totalScore(0)
                .build();
        return memberRepository.saveAndFlush(newMember);
    }
}
