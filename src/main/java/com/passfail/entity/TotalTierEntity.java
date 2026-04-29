package com.passfail.entity;

import java.time.LocalDateTime;
import com.passfail.enums.Tier;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "total_tier")
@Getter // 중요: 여기서 getTier() 메서드를 자동으로 생성함
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotalTierEntity {
    @Id
    private Long memberId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "member_id")
    private MemberEntity member;

    private Integer totalScore;
    private Integer currentRank;
    
    @Enumerated(EnumType.STRING)
    private Tier tier; // 필드 이름을 'tier'로 확정

    private LocalDateTime lastUpdatedAt;
}