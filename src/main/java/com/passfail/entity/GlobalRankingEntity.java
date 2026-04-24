package com.passfail.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import com.passfail.enums.Tier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 🌐 전체 등수 엔티티 (GlobalRanking)
 * ─────────────────────────────────────────────────────────────
 * ■ 역할
 *   - 전체 회원의 totalScore 기반 글로벌 순위를 관리
 *   - 데일리 티어(배틀) + 총 티어(문제 풀이) 점수를 합산한
 *     최종 통합 점수(combinedScore)로 순위 결정
 *   - 매일 자정 @Scheduled 배치로 순위/점수 갱신
 *
 * ■ 점수 합산 공식
 *   combinedScore = totalTierScore + dailyBonusScore
 *   (dailyBonusScore = 배틀 데일리 점수의 누적 가중치)
 *
 * ■ DB 저장 전략
 *   - 점수(Integer)로 저장 → Tier.fromScore() 로 티어 계산
 *   - 회원당 1개의 레코드 (UPSERT 방식으로 배치 갱신)
 * ─────────────────────────────────────────────────────────────
 */
@Entity
@Table(
	name = "global_ranking",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_global_ranking_member",
						  columnNames = {"member_id"})
	},
	indexes = {
		@Index(name = "idx_global_ranking_rank",  columnList = "globalRank"),
        @Index(name = "idx_global_ranking_score", columnList = "combinedScore"),
        @Index(name = "idx_global_ranking_tier",  columnList = "currentTier")
	}
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalRankingEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long globalRankingId;
	
	//회원 관련
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private MemberEntity member;
	
	@Column(name = "member_id", insertable = false, updatable = false)
	private Long memberId;
	
	@Column(nullable = false)
    @Builder.Default
	private Integer combinedScore = 0;
	
	@Column(nullable = false)
    @Builder.Default
	private Integer totalTierScore = 0;
	
	@Column(nullable = false)
    @Builder.Default
	private Integer dailyBonusScore = 0;
	
	@Column(nullable = false)
    @Builder.Default
	private Integer globalRank = 0;
	
	@Column(nullable = false)
    @Builder.Default
	private Integer previousRank = 0;
	
	@Column(nullable = false)
    @Builder.Default
	private Integer rankChange = 0;
	
	@Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
	@Builder.Default
	private Tier currentTier = Tier.BRONZE;
	
	@Column(nullable = false)
    @Builder.Default
	private Integer totalSolvedCount = 0;
	
	@Column(nullable = false)
    @Builder.Default
	private Integer totalBattleWin = 0;
	
	@Column(nullable = false)
    @Builder.Default
	private Integer totalBattleLose = 0;
	
	@Column(nullable = false)
    @Builder.Default
	private Double battleWinRate = 0.0;
	
	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;
	
	//편의 메서드
	public Tier resolvedTier() {
		
		return Tier.fromScore(this.combinedScore);
	}
	
	public String rankDirection() {
        if (this.rankChange > 0) return "UP";
        if (this.rankChange < 0) return "DOWN";
        return "SAME";
    }
	
	public void refreshByBatch(int newTotalTierScore, int newDailyBonusScore,
							   int newGlobalRank, int solvedCount,
							   int battleWin, int battleLose) {
		
		this.previousRank    = this.globalRank;
        this.totalTierScore  = newTotalTierScore;
        this.dailyBonusScore = newDailyBonusScore;
        this.combinedScore   = newTotalTierScore + newDailyBonusScore;
        this.rankChange      = this.previousRank - newGlobalRank;
        this.globalRank      = newGlobalRank;
        this.currentTier     = Tier.fromScore(this.combinedScore);
        this.totalSolvedCount = solvedCount;
        this.totalBattleWin   = battleWin;
        this.totalBattleLose  = battleLose;
        int total = battleWin + battleLose;
        this.battleWinRate = total > 0
                ? Math.round((battleWin / (double) total) * 10000.0) / 100.0
                : 0.0;
		
	}
	
	
}
