package com.passfail.member.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberJoinRequest {
    private String username;
    private String email;
    private String password;
    private String verificationCode;
    private Boolean privacyPolicyAgreed;
}
