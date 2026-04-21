package com.passfail.security;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class Oauth2UserService extends DefaultOAuth2UserService {  

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        String registration = userRequest.getClientRegistration().getRegistrationId();
                
        // 데이터 추출용 변수들 미리 선언
        String providerId = "";
        String nickname = "";
        String email = "";
        String image = "";

        Map<String, Object> attr = user.getAttributes();
        
        // 2. 플랫폼별 데이터 추출 및 변수 할당
        if (registration.equals("kakao")) {
            providerId = String.valueOf(attr.get("id"));
            Map<String, Object> account = (Map<String, Object>) attr.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) account.get("profile");
            nickname = (String) profile.get("nickname");
            image = (String) profile.get("profile_image_url");
            email = (String) account.get("email");
        } else if (registration.equals("google")) {
            providerId = (String) attr.get("sub");
            nickname = (String) attr.get("name");
            image = (String) attr.get("picture");
            email = (String) attr.get("email");
        } else if (registration.equals("naver")) {
            Map<String, Object> response = (Map<String, Object>) attr.get("response");
            providerId = (String) response.get("id");
            nickname = (String) response.get("name");
            image = (String) response.get("profile_image");
            email = (String) response.get("email");
        } else if (registration.equals("github")) {
            providerId = String.valueOf(attr.get("id"));
            nickname = (String) attr.get("login");
            image = (String) attr.get("avatar_url");
            email = (String) attr.get("email");        
        }
        
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
        String userNameAttr = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(authorities, user.getAttributes(), userNameAttr);
    }
}