package com.passfail.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.passfail.member.service.CustomOAuth2UserService;

import jakarta.servlet.DispatcherType;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(config -> config
        	.dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
        		// 2. 관리자(ADMIN)만 접근 가능한 경로
            .requestMatchers(
                "/ranking/update" // 🏆 랭킹 전체 갱신 API는 관리자 전용
            ).hasRole("ADMIN")
        	.requestMatchers(
                "/", "/login", "/signup", "/forgot-password", "/reset-password",
                "/api/member/**", "/api/auth/status", "/ranking", "/ranking/top"
            ).permitAll()
            .requestMatchers("/css/**", "/js/**", "/image/**").permitAll()
            .requestMatchers("/main", "/codingtest/**").permitAll()
            .requestMatchers("/board", "/posts/**").permitAll()
            .requestMatchers("/ranking/**", "/ranking-list").permitAll() // 🏆 랭킹 경로 추가 (누구나 볼 수 있게)
            .requestMatchers("/api/posts/**").authenticated()
            .requestMatchers(
            	"/ranking/me/**", // 🏆 내 랭킹 확인은 본인(로그인 유저)만
            	"/mypage/**",
                "/codingtest/run/**",
                "/codingtest/submit/**",
                "/codingtest/ai-review/**"
            ).authenticated()
            .anyRequest().authenticated()
        );

        http.formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/loginProc")
            .usernameParameter("username")
            .passwordParameter("password")
            .defaultSuccessUrl("/main", true)
            .permitAll()
        );

        http.oauth2Login(oauth -> oauth
            .loginPage("/login")
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
            .defaultSuccessUrl("/main", true)
        );

        http.logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}