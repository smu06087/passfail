package com.passfail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.passfail.enums.ActionType;

import java.time.LocalDateTime;

@Entity
@Table(name = "rebalance_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RebalanceLogEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

	@Column(name = "problem_id",nullable = false)
	private Long problemId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "problem_id", insertable = false, updatable = false)
	private ProblemEntity problem;

	@Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActionType actionType;

    @Column(nullable = false)
    private Double beforeAcceptance;

    private Double afterAcceptance;

    @Column()
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime rebalancedAt;
}