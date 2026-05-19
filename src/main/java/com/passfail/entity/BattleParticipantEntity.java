package com.passfail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.passfail.enums.BattleParticipantStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "battle_participant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleParticipantEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bpId;

	@Column(name = "room_id",nullable = false)
	private Long roomId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", insertable = false, updatable = false)
	private BattleRoomEntity room;

	@Column(name = "member_id",nullable = false)
	private Long memberId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", insertable = false, updatable = false)
	private MemberEntity member;
	
	@Column(nullable = false)
	private int slotIndex;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Builder.Default
	private BattleParticipantStatus status = BattleParticipantStatus.WAITING;

	private String currentNodeId;

	@Builder.Default
	private boolean isCleared = false;

	private Integer finalRank;

	private LocalDateTime finishedAt;

	private Integer score;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;
}
