package com.passfail.problem.repository;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.SolvedProblemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SolvedProblemRepository extends JpaRepository<SolvedProblemEntity, Long> {
    List<SolvedProblemEntity> findByMember(MemberEntity member);
    List<SolvedProblemEntity> findByMemberId(Long memberId);
}
