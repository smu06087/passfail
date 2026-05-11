package com.passfail.member.dto;

import com.passfail.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class OAuth2Member implements OAuth2User {
    private final OAuth2User oauth2User;
    private final String customName;
    private final Role role;
    private final Map<String, Object> attributes;

    public OAuth2Member(OAuth2User oauth2User, String customName, Role role) {
        this.oauth2User = oauth2User;
        this.customName = customName;
        this.role = role;
        this.attributes = oauth2User.getAttributes();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getName() {
        return customName; // 기존 providerId 대신 DB의 username 반환
    }

    public Role getRole() {
        return role;
    }

    public OAuth2User getOriginalUser() {
        return oauth2User;
    }
}
