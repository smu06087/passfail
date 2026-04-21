package com.passfail.entity.battle;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.passfail.entity.codingtest.ProblemEntity;
import com.passfail.entity.member.MemberEntity;
import com.passfail.enums.BattleRoomStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "battle_room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleRoomEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

	@Column(name = "host_id",nullable = false)
	private Long hostId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "host_id", insertable = false, updatable = false)
	private MemberEntity hostMember;

	@Column(name = "problem_id",nullable = false)
	private Long problemId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "problem_id", insertable = false, updatable = false)
	private ProblemEntity problem;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private BattleRoomStatus status = BattleRoomStatus.WAITING;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxParticipants = 4;

    private LocalDateTime startAt;

    private LocalDateTime end_at;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created_at;
    
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BattleParticipantEntity> participants = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BattleChatEntity> chats = new ArrayList<>();
}
