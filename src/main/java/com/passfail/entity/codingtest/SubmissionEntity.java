package com.passfail.entity.codingtest;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.passfail.entity.member.MemberEntity;
import com.passfail.enums.ProgrammingLanguage;
import com.passfail.enums.SubmissionStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "submission", uniqueConstraints = { @UniqueConstraint(columnNames = { "member_id", "problem_id" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long submissionId;

	@Column(name = "member_id",nullable = false)
	private Long memberId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", insertable = false, updatable = false)
	private MemberEntity member;
	
	@Column(name = "problem_id",nullable = false)
	private Long problemId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "problem_id", insertable = false, updatable = false)
	private ProblemEntity problem;

	@Column(nullable = false, length = 20)
	private ProgrammingLanguage language;

	@Lob 
	@Column(nullable = false)
	private String code;

	@Column(nullable = false, length = 30)
	private SubmissionStatus status;

	private Integer executionTimeMs;

	private Integer memoryUsedKb;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime submittedAt;
}
