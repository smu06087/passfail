package com.passfail.member.dto;

import com.passfail.entity.MemberEntity;
import com.passfail.enums.Tier;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberInfoResponse {
    private Long memberId;
    private String username;
    private String email;
    private Tier tier;
    private Integer pointBalance;
    private String profileImage;
    private List<String> socialProviders;
    private Integer totalScore;
    private Integer globalRank;
    private LocalDateTime createdAt;
    private boolean isOwnProfile;

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
