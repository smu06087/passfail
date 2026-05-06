package com.passfail.member.dto;

import lombok.*;

/**
 * 회원가입 요청 시 사용하는 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberJoinRequest {
    private String username;            // 사용자 ID (닉네임)
    private String email;               // 이메일
    private String password;            // 비밀번호
    private String verificationCode;    // 이메일 인증 코드
    private Boolean privacyPolicyAgreed; // 개인정보 동의 여부
}
