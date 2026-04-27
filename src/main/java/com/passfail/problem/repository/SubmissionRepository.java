package com.passfail.problem.repository;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.SubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<SubmissionEntity, Long> {
    List<SubmissionEntity> findByMemberId(Long memberId);
    List<SubmissionEntity> findByProblemId(Long problemId);
    List<SubmissionEntity> findByMemberOrderBySubmittedAtDesc(MemberEntity member);
    Optional<SubmissionEntity> findByMemberIdAndProblemId(Long memberId, Long problemId);
}
