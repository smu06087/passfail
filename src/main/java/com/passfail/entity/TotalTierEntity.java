package com.passfail.entity;

import java.time.LocalDateTime;
import com.passfail.enums.Tier;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "total_tier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotalTierEntity {
    
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TOTAL_TIER_ID")
    private Long totalTierId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID", nullable = false)
    private MemberEntity member;

    @Column(name = "TOTAL_SCORE", nullable = false)
    @Builder.Default
    private Integer totalScore = 0;
    
    @Column(name = "CURRENT_RANK", nullable = false)
    @Builder.Default
    private Integer currentRank = 0;

    @Column(name = "TIER", nullable = false, length = 255)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Tier tier = Tier.BRONZE;

    @Column(name = "LAST_UPDATED_AT")
    @Builder.Default
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();
    
    // 비즈니스 로직
    public void updateRankAndTier(Integer rank, Tier newTier) {
        this.currentRank = rank;
        this.tier = newTier;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public void addProblemScore(Integer addScore) {
        if (this.totalScore == null) {
            this.totalScore = 0;
        }
        this.totalScore += addScore;
        this.tier = Tier.fromScore(this.totalScore);
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public void addGameScore(Integer gameScore) {
        if (this.totalScore == null) {
            this.totalScore = 0;
        }
        this.totalScore += gameScore;
        this.tier = Tier.fromScore(this.totalScore);
        this.lastUpdatedAt = LocalDateTime.now();
    }
}