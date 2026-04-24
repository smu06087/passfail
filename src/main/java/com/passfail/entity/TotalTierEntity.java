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
 * 총 티어 엔티티
 * ─────────────────────────────────────────────────────────
 * - 문제 풀이 이력 기반의 누적 티어 (글로벌 랭킹과 연동)
 * - 배점 기준:
 *     ① 문제 난이도: EASY=10점 / MEDIUM=30점 / HARD=70점
 *     ② 제출 횟수 보정:
 *        - 1회 제출 정답  → 기본 점수 × 1.5 (보너스)
 *        - 2회 제출 정답  → 기본 점수 × 1.0
 *        - 3회 이상 정답  → 기본 점수 × 0.5 (감점)
 * - 매일 자정 @Scheduled 배치로 totalScore 재계산 후 업데이트
 * ─────────────────────────────────────────────────────────
 */

@Entity
@Table(
		name = "total_tier",
		uniqueConstraints = {
			//회원당 1개의 총 티어만 허용
			@UniqueConstraint(name = "uk_total_tier_member",
					columnNames = {"member_id"})	
		},
		indexes = {
				@Index(name = "idx_total_tier_member",     columnList = "member_id"),
		        @Index(name = "idx_total_tier_score",      columnList = "totalScore"),
		        @Index(name = "idx_total_tier_global_rank",columnList = "globalRank")
		}
		)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotalTierEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long totalTierId;
	
	//회원 연관
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private MemberEntity member;
	
	@Column(name = "member_id", insertable = false, updatable = false)
	private Long memberId;
	
	//총 점수 (DB저장 핵심 값)
	@Column(nullable = false)
	@Builder.Default
	private Integer totalScore = 0;       // 누적 총 점수
	
	//문제 풀이 통계
	@Column(nullable = false)
    @Builder.Default
	private Integer solvedEasyCount   = 0; // EASY 문제 정답 수
	
	@Column(nullable = false)
    @Builder.Default
	private Integer solvedMediumCount = 0; // MEDIUM 문제 정답 수
	
	@Column(nullable = false)
    @Builder.Default
	private Integer solvedHardCount   = 0; // HARD 문제 정답 수
	
	@Column(nullable = false)
    @Builder.Default
	private Integer firstTrySuccessCount = 0; // 첫 제출 정답 수 (고배점)
	
	@Column(nullable = false)
    @Builder.Default
	private Integer retrySuccessCount    = 0; // 재시도 정답 수 (저배점)
	
	//티어 점수기반 - DB캐시용
	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
    @Builder.Default
	private Tier totalTier = Tier.BRONZE; // Tier.fromScore(totalScore) 캐시
	
	//글로벌 랭킹
	@Column
	private Integer globalRank;           // 전체 순위 (배치 집계)
	
	//갱신시간
	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;      // 마지막 자정 배치 갱신 시간
	
	//편의 메서드
	
	//점수로 티어 실시간 계산
	public Tier resolvedTier() {
		
		return Tier.fromScore(this.totalScore);
	}
	
	/*
     * 문제 정답 시 점수 적용
     * @param baseScore    난이도 기본 점수 (EASY=10, MEDIUM=30, HARD=70)
     * @param submitCount  제출 횟수
     * @param isEasy       EASY 여부
     * @param isMedium     MEDIUM 여부
     */
	public void applyProblemSolve(int baseScore, int submitCount, 
			boolean isEasy, boolean isMedium, boolean isHard) {
		
		//제출 횟수 보정 계수
		double multiplier;
		if(submitCount == 1) {
			multiplier = 1.5;
			this.firstTrySuccessCount++;
		}else if(submitCount == 2) {
			multiplier = 1.0;
			this.retrySuccessCount++;
		}else {
			multiplier = 0.5;
			this.retrySuccessCount++;
		}
		
		int gained = (int) (baseScore * multiplier);
		this.totalScore += gained;
		this.totalTier = Tier.fromScore(this.totalScore);
		
		//난이도 카운트 증가
		if(isEasy) {
			this.solvedEasyCount++;
		}
		if(isMedium) {
			this.solvedMediumCount++;
		}
		if(isHard) {
			this.solvedHardCount++;
		}
		
	}
	
	//자정에 랭킹 변경
	public void refreshByBatch(int newTotalScore, int newGlobalRank) {
		
		this.totalScore = newTotalScore;
		this.globalRank = newGlobalRank;
		this.totalTier = Tier.fromScore(newTotalScore);
	}
	
}
