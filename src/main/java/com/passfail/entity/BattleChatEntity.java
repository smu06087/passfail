package com.passfail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "battle_chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleChatEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatId;

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
    private String message;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;
}
