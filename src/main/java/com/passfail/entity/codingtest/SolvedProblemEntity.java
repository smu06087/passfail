package com.passfail.entity.codingtest;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.passfail.entity.member.MemberEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "solved_problem", uniqueConstraints = { @UniqueConstraint(columnNames = { "member_id", "problem_id" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolvedProblemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long solvedId;

	@Column(name = "member_id",nullable = false)
	private Long memberId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", insertable = false, updatable = false)
	private MemberEntity member;
	
	@Column(name = "problem_Id",nullable = false)
	private Long problemId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "problem_Id", insertable = false, updatable = false)
	private ProblemEntity problem;

	@Column(nullable = false)
	@Builder.Default
	private Integer tryCount = 1;

	@Column(nullable = false)
	private Integer scoreEarned;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime firstSolvedAt;
}
