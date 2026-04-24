package com.passfail.entity;

import java.time.LocalDate;
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
 * 데일리 티어 엔티티
 * ─────────────────────────────────────────────────────────
 * - 배틀 결과를 기반으로 매일 집계되는 당일 티어
 * - 매일 자정 초기화(배치)
 * - DB에는 점수(dailyScore)로 저장 → Tier Enum으로 티어 계산
 * ─────────────────────────────────────────────────────────
 */
@Entity
@Table(
		name = "daily_tier",
		uniqueConstraints = {
				//회원당 날짜별 1개의 데일리 티어 레코드만 허용
				@UniqueConstraint(name = "uk_daily_tier_member_date",
						columnNames = {"member_id", "tierDate"})
			},
		indexes = {
				@Index(name = "idx_daily_tier_member",  columnList = "member_id"),
		        @Index(name = "idx_daily_tier_date",    columnList = "tierDate"),
		        @Index(name = "idx_daily_tier_rank",    columnList = "dailyRank")
			}
		)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyTierEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long dailyTierId;
	
	// 회원 연관 member_id는 읽기 전용
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private MemberEntity member;
	
	// 인서트 업데이트 금지 중복 방지 조회용
	@Column(name = "member_id", insertable = false, updatable = false)
	private Long memberId;
	
	// 날짜
	@Column(nullable = false)
	private LocalDate tierDate;
	
	// 베틀 결과 집계
	@Column(nullable = false)
	@Builder.Default
	private Integer battleWinCount  = 0; // 당일 배틀 승리 횟수
	
	@Column(nullable = false)
	@Builder.Default
	private Integer battleLoseCount = 0; // 당일 배틀 패배 횟수
	
	@Column(nullable = false)
	@Builder.Default
	private Integer battleDrawCount = 0; // 당일 배틀 무승부 횟수
	
	// 점수
	@Column(nullable = false)
	@Builder.Default
	private Integer dailyScore = 0;      // 당일 배틀 누적 점수
	
	// 티어 (점수기반 동적계산 - DB에는 캐시용으로 저장)
	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private Tier dailyTier = Tier.BRONZE; // Tier.fromScore(dailyScore) 결과 캐시
	
	// 랭킹
	@Column
	private Integer dailyRank;           // 당일 랭킹 (배치로 집계)
	
	// 타임스템프
	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;
	
	// 편의 메서드
	
	//점수로 티어를 실시간 계산(Enum에 위임)
	public Tier resolvedTier() {
		
		return Tier.fromScore(this.dailyScore);
	}
	
	//승리시 점수 - 카운트 업데이트
	public void applyBattleWin(int gainScore) {
		
		this.battleWinCount++;
		this.dailyScore += gainScore;
		this.dailyTier = Tier.fromScore(this.dailyScore);
	}
	
	//패배시 점수 - 카운트 업데이트 
	public void applyBattleLose(int loseScore) {
		
		this.battleLoseCount++;
		this.dailyScore = Math.max(0, this.dailyScore - loseScore);
		this.dailyTier = Tier.fromScore(this.dailyScore);
	}
	
	//무승부 시 카운트 업데이트
	public void applyBattleDraw() {
		
		this.battleDrawCount++;
	}
	
	
}
