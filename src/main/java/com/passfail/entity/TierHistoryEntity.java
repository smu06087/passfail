package com.passfail.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.passfail.enums.Tier;
import com.passfail.enums.TierChangeReason;
import com.passfail.enums.TierType;
import com.sun.istack.Nullable;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *티어 변동 이력 엔티티
 * ─────────────────────────────────────────────────────────
 * - 데일리 티어 & 총 티어 변동을 모두 기록
 * - tierType 으로 어느 티어의 이력인지 구분
 * - DB에는 점수(before/after)로 저장, Enum으로 티어 판단
 * ─────────────────────────────────────────────────────────
 */
@ Entity
@Table(
	name = "tier_history",
	indexes = {
		@Index(name = "idx_tier_history_member",    columnList = "member_id"),
        @Index(name = "idx_tier_history_type_date", columnList = "tierType, changedAt")
		}
	)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierHistoryEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long tierHistoryId;
	
	//회원 연관
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private MemberEntity member;
	
	@Column(name = "member_id", insertable = false, updatable = false)
	private Long memberId;
	
	//티어 구분
	@Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
	private TierType tierType;           // DAILY or TOTAL
	
	//점수 변동 
	@Column(nullable = false)
	private Integer scoreBefore;         // 변경 전 점수
	
	@Column(nullable = false)
	private Integer scoreAfter;          // 변경 후 점수
	
	@Column(nullable = false)
	private Integer scoreDelta;          // 점수 변화량 (양수/음수)
	
	//티어 변동
	@Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
	private Tier tierBefore;             // 변경 전 티어
	
	@Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
	private Tier tierAfter;              // 변경 후 티어
	
	@Column(nullable = false, length = 20)
    @Builder.Default
	private Boolean isTierChanged = false; // 티어 등급 자체가 바뀌었는지
	
	//변동 원인
	@Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
	private TierChangeReason reason;
	
	@Column(length = 255)
	private String memo;
	
	//시간
	@CreationTimestamp
    @Column(nullable = false, updatable = false)
	private LocalDateTime changedAt;
	
	//편의 메서드
	
	//이력 생성 팩토리 메서드
	public static TierHistoryEntity of(Long memberId, TierType tierType, 
			int scoreBefore, int scoreAfter, TierChangeReason reason, String memo) {
		
		Tier before = Tier.fromScore(scoreBefore);
		Tier after = Tier.fromScore(scoreAfter);
		return TierHistoryEntity.builder().memberId(memberId).tierType(tierType).
				scoreBefore(scoreBefore).scoreAfter(scoreAfter).scoreDelta(scoreAfter - scoreBefore).
				tierBefore(before).tierAfter(after).isTierChanged(!before.equals(after)).reason(reason).memo(memo).build();
	}
	
}
