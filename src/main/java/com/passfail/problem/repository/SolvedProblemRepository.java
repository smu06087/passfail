package com.passfail.problem.repository;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.SolvedProblemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface SolvedProblemRepository extends JpaRepository<SolvedProblemEntity, Long> {
    List<SolvedProblemEntity> findByMember(MemberEntity member);
    Page<SolvedProblemEntity> findByMember(MemberEntity member, Pageable pageable);
    List<SolvedProblemEntity> findByMemberId(Long memberId);
}
