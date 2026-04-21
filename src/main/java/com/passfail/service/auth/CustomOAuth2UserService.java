package com.passfail.service.auth;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.passfail.dto.member.MemberDTO;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        // 어떤 소셜 서비스인지 확인 (google, kakao, naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        String providerId = "";
        String nickname = "";
        String email = "";
        String profileImage = "";

        // 1. 카카오(Kakao) 데이터 추출
        if ("kakao".equals(registrationId)) {
            providerId = oAuth2User.getAttribute("id").toString();
            Map<String, Object> properties = (Map<String, Object>) oAuth2User.getAttribute("properties");
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttribute("kakao_account");

            if (properties != null) {
                nickname = (String) properties.get("nickname");
                profileImage = (String) properties.get("profile_image");
            }
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
            }
        } 
        // 2. 구글(Google) 데이터 추출
        else if ("google".equals(registrationId)) {
            providerId = oAuth2User.getAttribute("sub");
            nickname = oAuth2User.getAttribute("name");
            email = oAuth2User.getAttribute("email");
            profileImage = oAuth2User.getAttribute("picture");
        }
        // 3. 네이버(Naver) 데이터 추출 (추가)
        else if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) oAuth2User.getAttribute("response");
            if (response != null) {
                providerId = (String) response.get("id");
                nickname = (String) response.get("nickname");
                email = (String) response.get("email");
                profileImage = (String) response.get("profile_image");
            }
        }
        // 4. 깃허브(GitHub) 데이터 추출 추가!
        else if ("github".equals(registrationId)) {
            // 깃허브 고유 ID는 'id' 키에 담겨 있으며 숫자 타입이므로 toString() 처리
            Object githubId = oAuth2User.getAttribute("id");
            providerId = (githubId != null) ? githubId.toString() : "";
            
            nickname = oAuth2User.getAttribute("login"); // 깃허브는 'login'이 사용자명입니다.
            email = oAuth2User.getAttribute("email");
            profileImage = oAuth2User.getAttribute("avatar_url"); // 프로필 이미지는 'avatar_url'입니다.
        }

        // 닉네임이 없는 경우를 대비한 방어 코드 (테이블 NOT NULL 제약조건 때문)
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = registrationId + "_user_" + providerId.substring(0, Math.min(providerId.length(), 5));
        }

        return oAuth2User;
    }
}