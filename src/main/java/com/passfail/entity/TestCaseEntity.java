package com.passfail.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_case")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long caseId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "problem_id", nullable = false)
	private ProblemEntity problem;
	
	@Column(columnDefinition = "LONGTEXT", nullable = false)
	private String inputData;
	
	@Column(columnDefinition = "LONGTEXT", nullable = false)
	private String expectedOutput;

	@Column(nullable = false)
	@Builder.Default
	private Boolean isSample = false;

	@Column(nullable = false)
	@Builder.Default
	private Integer orderNum = 1;
}
