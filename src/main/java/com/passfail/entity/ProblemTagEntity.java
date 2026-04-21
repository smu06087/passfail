package com.passfail.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "problem_tag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemTagEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tagId;

	@Column(name = "problem_id",nullable = false)
	private Long problemId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "problem_id", insertable = false, updatable = false)
	private ProblemEntity problem;

    @Column(nullable = false, length = 50)
    private String tagName;
}
