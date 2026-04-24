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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 📜 전체 등수 변동 이력 엔티티
 * ─────────────────────────────────────────────────────────────
 * ■ 역할
 *   - 매일 자정 배치 실행 시 GlobalRankingEntity 의 스냅샷을 기록
 *   - 날짜별 순위/점수/티어 변동 추이를 조회할 때 사용
 *   - 랭킹 그래프, 마이페이지 히스토리 뷰에 활용
 * ─────────────────────────────────────────────────────────────
 */
@Entity
@Table(
	name = "global_ranking_history",
	indexes = {
		@Index(name = "idx_grh_member",      columnList = "member_id"),
        @Index(name = "idx_grh_record_date", columnList = "recordedDate"),
        @Index(name = "idx_grh_member_date", columnList = "member_id, recordedDate")
	}
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalRankingHistoryEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long historyId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private MemberEntity member;
	
	@Column(name = "member_id", insertable = false, updatable = false)
	private Long memberId;
	
	@Column(nullable = false)
	private LocalDate recordedDate;
	
	@Column(nullable = false)
	private Integer combinedScoreSnapshot;
	
	@Column(nullable = false)
	private Integer totalTierScoreSnapshot;
	
	@Column(nullable = false)
	private Integer dailyBonusScoreSnapshot;
	
	@Column(nullable = false)
	private Integer rankSnapshot;
	
	@Column(nullable = false)
	private Integer rankChangeSnapshot;
	
	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private Tier tierSnapshot;
	
	@UpdateTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;
	
	//팩토리 메서드
	public static GlobalRankingHistoryEntity snapshotFrom(GlobalRankingEntity entity) {
        return GlobalRankingHistoryEntity.builder()
                .member                 (entity.getMember())
                .recordedDate           (LocalDate.now())
                .combinedScoreSnapshot  (entity.getCombinedScore())
                .totalTierScoreSnapshot (entity.getTotalTierScore())
                .dailyBonusScoreSnapshot(entity.getDailyBonusScore())
                .rankSnapshot           (entity.getGlobalRank())
                .rankChangeSnapshot     (entity.getRankChange())
                .tierSnapshot           (entity.getCurrentTier())
                .build();
    }
	
}
