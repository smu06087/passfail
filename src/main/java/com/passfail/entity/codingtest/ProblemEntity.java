package com.passfail.entity.codingtest;

import jakarta.persistence.*;  
import lombok.*;  
import org.hibernate.annotations.CreationTimestamp;  
import org.hibernate.annotations.UpdateTimestamp;

import com.passfail.entity.member.MemberEntity;
import com.passfail.enums.Difficulty;
import com.passfail.enums.ProblemStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;  

@Entity
@Table(name = "problem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long problemId;

    @Column(name = "created_by",nullable = false)
    private Long createdBy;
    @ManyToOne( fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private MemberEntity creator;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob 
    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    @Builder.Default
    private Integer timeLimitMs = 1000;

    @Column(nullable = false)
    @Builder.Default
    private Integer memoryLimitMb = 256;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProblemStatus status = ProblemStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private Double acceptanceRate = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer submissionCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer acceptedCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Builder.Default
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCaseEntity> test_cases = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProblemTagEntity> tags = new ArrayList<>();
}
