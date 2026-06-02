package com.passfail.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
            .requestMatchers("/ranking/update").hasRole("ADMIN")
            .requestMatchers(
                "/", "/login", "/signup", "/forgot-password", "/reset-password",
                "/api/member/**", "/api/auth/status", "/api/chat/send", "/ranking", "/ranking/top",
                "/member/set-username"
            ).permitAll()
            .requestMatchers("/css/**", "/js/**", "/image/**").permitAll()
            .requestMatchers("/main", "/codingtest/**").permitAll()
            .requestMatchers("/board", "/posts/**").permitAll()
            .requestMatchers("/ranking/**", "/ranking-list").permitAll()
            .requestMatchers("/ai/**").permitAll()
            .requestMatchers("/api/posts/**").authenticated()
            .requestMatchers(
                "/ranking/me/**",
                "/mypage/**",
                "/codingtest/run/**",
                "/codingtest/submit/**",
                "/codingtest/ai-review/**"
            ).authenticated()
            .requestMatchers("/admin/**", "/api/admin/**").permitAll()
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

        http.sessionManagement(session -> session
            .maximumSessions(1)
            .maxSessionsPreventsLogin(false)
            .expiredUrl("/login?expired")
        );

        http.exceptionHandling(exceptions -> exceptions
            .defaultAuthenticationEntryPointFor(
                (request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("""
                        {"success":false,"message":"로그인이 필요합니다."}
                        """);
                },
                new AntPathRequestMatcher("/ai/**")
            )
            .defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                new AntPathRequestMatcher("/api/**")
            )
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.security.web.session.HttpSessionEventPublisher httpSessionEventPublisher() {
        return new org.springframework.security.web.session.HttpSessionEventPublisher();
    }
}
