package com.passfail.member.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Spring Security의 OAuth2User를 래핑하는 커스텀 클래스
 * 소셜 로그인 사용자에게 DB에 저장된 실제 username과 권한(Role)을 제공하기 위해 사용됩니다.
 */
public class OAuth2Member implements OAuth2User {
    private final OAuth2User oauth2User;
    private final String customName;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    public OAuth2Member(OAuth2User oauth2User, String customName, Collection<? extends GrantedAuthority> authorities) {
        this.oauth2User = oauth2User;
        this.customName = customName;
        this.attributes = oauth2User.getAttributes();
        this.authorities = authorities;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * @return DB에서 조회된 권한 목록을 반환
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
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
