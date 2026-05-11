package com.passfail.member.dto;

import com.passfail.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Spring Security의 OAuth2User를 래핑하는 커스텀 클래스
 * 소셜 로그인 사용자에게 DB에 저장된 실제 username과 권한(Role)을 제공하기 위해 사용됩니다.
 */
public class OAuth2Member implements OAuth2User {
    private final OAuth2User oauth2User;
    private final String customName;
    private final Role role;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

<<<<<<< HEAD
    public OAuth2Member(OAuth2User oauth2User, String customName, Role role) {
=======
    public OAuth2Member(OAuth2User oauth2User, String customName, Collection<? extends GrantedAuthority> authorities) {
>>>>>>> 0008cc0d756cead770e15ffbea97496d853c5abf
        this.oauth2User = oauth2User;
        this.customName = customName;
        this.role = role;
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
<<<<<<< HEAD
        return Collections.singleton(new SimpleGrantedAuthority(role.name()));
=======
        return authorities;
>>>>>>> 0008cc0d756cead770e15ffbea97496d853c5abf
    }

    /**
     * @return 소셜 서비스의 고유 ID 대신 DB에 저장된 사용자 닉네임을 반환
     */
    @Override
    public String getName() {
        return customName;
    }

    public Role getRole() {
        return role;
    }

    public OAuth2User getOriginalUser() {
        return oauth2User;
    }
}
