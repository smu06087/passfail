package com.passfail.problem.repository;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.SubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<SubmissionEntity, Long> {
    List<SubmissionEntity> findByMemberId(Long memberId);
    List<SubmissionEntity> findByProblemId(Long problemId);
    List<SubmissionEntity> findByMemberOrderBySubmittedAtDesc(MemberEntity member);
    Page<SubmissionEntity> findByMember(MemberEntity member, Pageable pageable);
    Optional<SubmissionEntity> findFirstByMemberIdAndProblemIdOrderBySubmittedAtDesc(Long memberId, Long problemId);
}
