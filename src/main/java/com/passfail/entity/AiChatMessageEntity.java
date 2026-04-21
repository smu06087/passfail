package com.passfail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.passfail.enums.AiChatRole;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatMessageEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

	@Column(name = "session_id",nullable = false)
	private Long sessionId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id", insertable = false, updatable = false)
	private AiChatSessionEntity session;

	@Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiChatRole role; // USER / ASSISTANT

    @Lob 
    @Column(nullable = false)
    private String content;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;
}
