package com.passfail.problem.repository;

import com.passfail.entity.HintUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HintUsageRepository extends JpaRepository<HintUsageEntity, Long> {
    List<HintUsageEntity> findByMemberId(Long memberId);
    Optional<HintUsageEntity> findByMemberIdAndProblemId(Long memberId, Long problemId);
}
