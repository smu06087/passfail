package com.passfail.entity.ai;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.passfail.entity.member.MemberEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_code_review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCodeReviewEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

	@Column(name = "member_id",nullable = false)
	private Long memberId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", insertable = false, updatable = false)
	private MemberEntity member;

	@Column(name = "session_id",nullable = false)
	private Long sessionId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id", insertable = false, updatable = false)
	private AiChatSessionEntity session;

	@Lob 
    @Column(nullable = false)
    private String reviewContent;

    @Column(nullable = false)
    @Builder.Default
    private Integer pointUsed = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isFree = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
