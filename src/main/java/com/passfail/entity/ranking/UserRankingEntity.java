package com.passfail.entity.ranking;

import com.passfail.entity.member.MemberEntity;
import com.passfail.enums.Tier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_ranking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRankingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rankingId;

	@Column(name = "member_id",nullable = false)
	private Long memberId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", insertable = false, updatable = false)
	private MemberEntity member;

    @Column(nullable = false)
    private Integer globalRank;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalScore = 0;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private Tier tier = Tier.BRONZE;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
