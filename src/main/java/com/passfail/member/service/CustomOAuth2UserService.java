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

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

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

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Provider provider = Provider.valueOf(registrationId.toUpperCase());
        
        // 1. 소셜 데이터 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String providerId = getProviderId(registrationId, attributes);
        String nickname = getNickname(registrationId, attributes);
        String email = getEmail(registrationId, attributes);
        String profileImage = getProfileImage(registrationId, attributes);

        if (nickname == null || nickname.isEmpty()) nickname = registrationId + "_" + providerId;
        
        final String nicknameForLambda = nickname;
        final String emailForLambda = email;
        final String profileImageForLambda = profileImage;

        // 2. 현재 로그인된 유저(연동 주체) 확인
        MemberEntity currentMember = getCurrentLoggedInMember();

        // 3. 소셜 정보로 기존 연동 이력 조회
        Optional<SocialAccountEntity> socialOpt = socialAccountRepository.findByProviderAndProviderId(provider, providerId);
        
        MemberEntity finalMember;

        if (currentMember != null) {
            // [연동 모드] 이미 로그인된 상태에서 새로운 소셜 계정 연결 시도
            finalMember = currentMember;
            
            if (socialOpt.isPresent()) {
                SocialAccountEntity existingSocial = socialOpt.get();
                if (!existingSocial.getMemberId().equals(currentMember.getMemberId())) {
                    log.info("Social account {} ownership transferred to {}", provider, currentMember.getMemberId());
                    existingSocial.setMemberId(currentMember.getMemberId());
                    existingSocial.setMembers(currentMember);
                    socialAccountRepository.save(existingSocial);
                }
            } else {
                // 처음 연동하는 소셜 계정: 해제 시 돌아갈 "베이스 멤버"가 있는지 확인 후 생성
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
                // 1. members 테이블에 이 소셜 계정 고유의 레코드(Base Member)를 먼저 생성 (연동 해제 대비)
                String baseEmail = provider.name().toLowerCase() + "_" + providerId + "@passfail.com";
                MemberEntity baseMember = createNewMember(nicknameForLambda, baseEmail, profileImageForLambda);
                
                // 2. 소셜 계정의 실제 이메일과 동일한 로컬 계정이 있는지 확인
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
                // social_account 테이블에 연동 정보 저장
                saveSocialAccount(finalMember, provider, providerId);
            }
        }

        // 공통: 프로필 이미지가 없는 경우에만 소셜 프로필로 설정 (최초 로그인 시의 프로필 유지)
        if (profileImage != null && (finalMember.getProfileImage() == null || finalMember.getProfileImage().isEmpty())) {
            finalMember.setProfileImage(profileImage);
            memberRepository.save(finalMember);
        }

        return new OAuth2Member(oAuth2User, finalMember.getUsername(), finalMember.getRole());
    }

    private void ensureBaseMemberExists(String nickname, String profileImage, Provider provider, String providerId) {
        if (socialAccountRepository.findByProviderAndProviderId(provider, providerId).isEmpty()) {
            String baseEmail = provider.name().toLowerCase() + "_" + providerId + "@passfail.com";
            createNewMember(nickname, baseEmail, profileImage);
        }
    }

    private MemberEntity getCurrentLoggedInMember() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || (auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            return null;
        }
        return memberRepository.findByUsername(auth.getName()).orElse(null);
    }

    private String getProviderId(String regId, Map<String, Object> attr) {
        if ("kakao".equals(regId)) return attr.get("id").toString();
        if ("google".equals(regId)) return (String) attr.get("sub");
        if ("naver".equals(regId)) return (String) ((Map) attr.get("response")).get("id");
        if ("github".equals(regId)) return attr.get("id").toString();
        return "";
    }

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

    private void saveSocialAccount(MemberEntity member, Provider provider, String providerId) {
        SocialAccountEntity socialAccount = SocialAccountEntity.builder()
                .memberId(member.getMemberId())
                .members(member)
                .provider(provider)
                .providerId(providerId)
                .build();
        socialAccountRepository.saveAndFlush(socialAccount);
    }

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
                .pointBalance(0)
                .totalScore(0)
                .build();
        return memberRepository.saveAndFlush(newMember);
    }
}
