package com.passfail.problem.service;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.ProblemEntity;
import com.passfail.entity.SolvedProblemEntity;
import com.passfail.entity.SubmissionEntity;
import com.passfail.enums.ProblemStatus;
import com.passfail.enums.ProgrammingLanguage;
import com.passfail.enums.SubmissionStatus;
import com.passfail.enums.Tier;
import com.passfail.member.repository.MemberRepository;
import com.passfail.problem.dto.ProblemResponse;
import com.passfail.problem.dto.TestCaseResponse;
import com.passfail.problem.repository.ProblemRepository;
import com.passfail.problem.repository.SolvedProblemRepository;
import com.passfail.problem.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final MemberRepository memberRepository;
    private final SolvedProblemRepository solvedProblemRepository;

    public List<ProblemResponse> getActiveProblems() {
        return problemRepository.findByStatus(ProblemStatus.PUBLISHED).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ProblemResponse getProblemResponse(Long id) {
        ProblemEntity problem = problemRepository.findByIdWithTestCases(id)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));
        return convertToResponse(problem);
    }

    public ProblemEntity getProblemEntity(Long id) {
        return problemRepository.findByIdWithTestCases(id)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));
    }

    @Transactional
    public SubmissionEntity submitSolution(String username, Long problemId, String code, ProgrammingLanguage language) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        ProblemEntity problem = getProblemEntity(problemId);

        // 제출 기록 업데이트 또는 생성
        SubmissionEntity submission = submissionRepository.findByMemberIdAndProblemId(member.getMemberId(), problemId)
                .orElse(SubmissionEntity.builder()
                        .memberId(member.getMemberId())
                        .problemId(problem.getProblemId())
                        .build());
        
        submission.setCode(code);
        submission.setLanguage(language);
        submission.setStatus(SubmissionStatus.ACCEPTED);
        submission.setExecutionTimeMs(100);
        submission.setMemoryUsedKb(1024);
        
        submission = submissionRepository.save(submission);

        // 해결한 문제 기록 업데이트
        solvedProblemRepository.findByMemberId(member.getMemberId()).stream()
                .filter(sp -> sp.getProblemId().equals(problemId))
                .findFirst()
                .ifPresentOrElse(
                    sp -> {
                        sp.setTryCount(sp.getTryCount() + 1);
                    },
                    () -> {
                        int score = 100; // 기본 점수
                        SolvedProblemEntity solvedProblem = SolvedProblemEntity.builder()
                                .memberId(member.getMemberId())
                                .problemId(problemId)
                                .scoreEarned(score)
                                .tryCount(1)
                                .build();
                        solvedProblemRepository.save(solvedProblem);
                        
                        member.setTotalScore(member.getTotalScore() + score);
                        member.setTier(Tier.fromScore(member.getTotalScore()));
                        memberRepository.save(member);
                    }
                );

        problem.setSubmissionCount(problem.getSubmissionCount() + 1);
        problem.setAcceptedCount(problem.getAcceptedCount() + 1);
        problem.setAcceptanceRate((double) problem.getAcceptedCount() / (double) problem.getSubmissionCount() * 100.0);
        problemRepository.save(problem);
        
        return submission;
    }

    @Transactional(readOnly = true)
    public String getPreviousSolution(String username, Long problemId) {
        MemberEntity member = memberRepository.findByUsername(username).orElse(null);
        if (member == null) return null;
        
        return submissionRepository.findByMemberIdAndProblemId(member.getMemberId(), problemId)
                .map(SubmissionEntity::getCode)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isSolved(String username, Long problemId) {
        MemberEntity member = memberRepository.findByUsername(username).orElse(null);
        if (member == null) return false;
        
        return solvedProblemRepository.findByMemberId(member.getMemberId()).stream()
                .anyMatch(sp -> sp.getProblemId().equals(problemId));
    }

    private ProblemResponse convertToResponse(ProblemEntity entity) {
        return ProblemResponse.builder()
                .problemId(entity.getProblemId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .difficulty(entity.getDifficulty().name())
                .category(entity.getCategory())
                .timeLimitMs(entity.getTimeLimitMs())
                .memoryLimitMb(entity.getMemoryLimitMb())
                .acceptanceRate(entity.getAcceptanceRate())
                .testCases(entity.getTest_cases() != null ? entity.getTest_cases().stream()
                        .map(tc -> TestCaseResponse.builder()
                                .caseId(tc.getCaseId())
                                .inputData(tc.getInputData())
                                .expectedOutput(tc.getExpectedOutput())
                                .isSample(tc.getIsSample())
                                .build())
                        .collect(Collectors.toList()) : List.of())
                .build();
    }
}
