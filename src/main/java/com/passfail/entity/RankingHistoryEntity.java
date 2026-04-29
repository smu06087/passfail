package com.passfail.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import com.passfail.enums.Tier;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 📜 랭킹 변동 이력 엔티티 (개편됨)
 * ─────────────────────────────────────────────────────────────
 * ■ 역할
 *   - 매일 자정 배치 실행 시 TotalTierEntity의 상태를 스냅샷으로 기록
 *   - 날짜별 순위/점수/티어 변동 추이를 조회할 때 사용
 * ─────────────────────────────────────────────────────────────
 */
@Entity
@Table(
    name = "ranking_history", // 테이블명 변경
    indexes = {
        @Index(name = "idx_rh_member",      columnList = "member_id"),
        @Index(name = "idx_rh_record_date", columnList = "recorded_date"),
        @Index(name = "idx_rh_member_date", columnList = "member_id, recorded_date")
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingHistoryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;
    
    @Column(nullable = false)
    private LocalDate recordedDate;
    
    @Column(nullable = false)
    private Integer scoreSnapshot; // 통합된 점수 스냅샷
    
    @Column(nullable = false)
    private Integer rankSnapshot; // 당시 순위
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Tier tierSnapshot; // 당시 티어
    
    @UpdateTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
 // RankingHistoryEntity.java 파일 하단의 팩토리 메서드 부분만 수정하세요.

    public static RankingHistoryEntity snapshotFrom(TotalTierEntity totalTier, Integer currentRank) {
        return RankingHistoryEntity.builder()
                .member(totalTier.getMember())
                .recordedDate(LocalDate.now())
                .scoreSnapshot(totalTier.getTotalScore())
                .rankSnapshot(currentRank)
                .tierSnapshot(totalTier.getTier()) // ★ 이 부분을 .getTier()로 반드시 수정하세요!
                .build();
    }
}