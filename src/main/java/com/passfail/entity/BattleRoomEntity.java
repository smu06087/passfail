package com.passfail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.passfail.enums.BattleRoomStatus;
import com.passfail.enums.Difficulty;

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

    @Column(nullable = false)
    private String roomName;
    
    @Column()
    private String password;
	
    @Column(nullable = false)
	private Long battleSeed;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "battle_mode", nullable = false, length = 20)
    @Builder.Default
    private com.passfail.enums.BattleMode battleMode = com.passfail.enums.BattleMode.QUICK;

    @Column(name = "problem_id")
    private Long problemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

	@Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BattleRoomStatus status = BattleRoomStatus.WAITING;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxParticipants = 4;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private LocalDateTime actualStartedAt;

    @Column(length = 255)
    private String tags;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BattleParticipantEntity> participants = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BattleChatEntity> chats = new ArrayList<>();
}
