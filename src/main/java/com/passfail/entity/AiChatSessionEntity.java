package com.passfail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_chat_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatSessionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sessionId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", insertable = false, updatable = false)
	private MemberEntity member;

	@Column(name = "problem_id")
	private Long problemId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "problem_id", insertable = false, updatable = false)
	private ProblemEntity problem;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime startedAt;

	private LocalDateTime endedAt;

	@Builder.Default
	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<AiChatMessageEntity> messages = new ArrayList<>();
}
