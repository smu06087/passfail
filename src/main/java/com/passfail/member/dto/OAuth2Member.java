package com.passfail.member.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Spring Security의 OAuth2User를 래핑하는 커스텀 클래스
 * 소셜 로그인 사용자에게 DB에 저장된 실제 username을 제공하기 위해 사용됩니다.
 */
public class OAuth2Member implements OAuth2User {
    private final OAuth2User oauth2User;
    private final String customName;
    private final Map<String, Object> attributes;

    public OAuth2Member(OAuth2User oauth2User, String customName) {
        this.oauth2User = oauth2User;
        this.customName = customName;
        this.attributes = oauth2User.getAttributes();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    /**
     * @return 소셜 서비스의 고유 ID 대신 DB에 저장된 사용자 닉네임을 반환
     */
    @Override
    public String getName() {
        return customName;
    }

    public OAuth2User getOriginalUser() {
        return oauth2User;
    }
}
