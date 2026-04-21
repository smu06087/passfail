package com.passfail.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.passfail.auth.service.CustomOAuth2UserService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(config -> config
            .requestMatchers("/**").permitAll()
            .requestMatchers("/forgot-password", "/reset-password").permitAll()
            .requestMatchers("/css/**", "/js/**", "/image/**").permitAll()
            .anyRequest().authenticated()
        );

        // 1. 로컬 로그인(폼 로그인) 설정
        http.formLogin(form -> form
            .loginPage("/login")                // 로그인 페이지 경로
            .loginProcessingUrl("/loginProc")   // <form action="/loginProc"> 와 일치해야 함
            .usernameParameter("username")      // input name="username"
            .passwordParameter("password")      // input name="password"
            .defaultSuccessUrl("/home", true)
            .permitAll()
        );

        // 2. 소셜 로그인 설정 (기존 유지)
        http.oauth2Login(oauth -> oauth
            .loginPage("/login")
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
            .defaultSuccessUrl("/home", true)
        );

        http.logout(logout -> logout
            .logoutUrl("/logout") // 로그아웃을 요청할 주소
            .logoutSuccessUrl("/login") // 로그아웃 성공 후 이동할 페이지
            .invalidateHttpSession(true) // HTTP 세션 무효화
            .deleteCookies("JSESSIONID") // 쿠키 삭제
            .permitAll()
        );
        
        
        return http.build();
    }

    // ⭐ 비밀번호 암호화 객체 등록 (로컬 로그인 필수)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
