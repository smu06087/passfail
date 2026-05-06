package com.passfail.member.dto;

import com.passfail.entity.MemberEntity;
import com.passfail.enums.Tier;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 회원 상세 정보를 전달하기 위한 응답 DTO
 * 마이페이지 프로필 조회 시 사용됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberInfoResponse {
    private Long memberId;          // 회원 고유 번호
    private String username;        // 사용자 ID (닉네임)
    private String email;           // 이메일 주소
    private Tier tier;              // 현재 티어
    private Integer pointBalance;   // 보유 포인트
    private String profileImage;    // 프로필 이미지 URL
    private List<String> socialProviders; // 연동된 소셜 서비스 목록 (GOOGLE, KAKAO 등)
    private Integer totalScore;     // 총 점수
    private Integer globalRank;     // 전체 순위
    private LocalDateTime createdAt; // 가입 일시
    private boolean isOwnProfile;   // 본인 프로필 여부

    /**
     * Entity를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static MemberInfoResponse from(MemberEntity entity, boolean isOwnProfile) {
        return MemberInfoResponse.builder()
                .memberId(entity.getMemberId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .tier(entity.getTier())
                .pointBalance(entity.getPointBalance())
                .profileImage(entity.getProfileImage())
                .totalScore(entity.getTotalScore())
                .globalRank(entity.getGlobalRank())
                .createdAt(entity.getCreatedAt())
                .isOwnProfile(isOwnProfile)
                .socialProviders(entity.getSocial_accounts().stream()
                        .map(sa -> sa.getProvider().name())
                        .collect(Collectors.toList()))
                .build();
    }
}
