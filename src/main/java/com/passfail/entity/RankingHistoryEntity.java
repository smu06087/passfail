package com.passfail.entity;

import com.passfail.enums.Tier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "ranking_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

	@Column(name = "member_id",nullable = false)
	private Long memberId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", insertable = false, updatable = false)
	private MemberEntity member;

    @Column(nullable = false)
    private Integer rankSnapshot;

    @Column(nullable = false)
    private Integer scoreSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tier tierSnapshot;

    @Column(nullable = false)
    private LocalDate recordedDate;
}