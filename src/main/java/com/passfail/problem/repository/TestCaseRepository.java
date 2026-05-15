package com.passfail.problem.repository;

import com.passfail.entity.TestCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCaseEntity, Long> {
    List<TestCaseEntity> findByProblem_ProblemIdAndIsSampleTrueOrderByOrderNumAsc(Long problemId);

    List<TestCaseEntity> findByProblem_ProblemIdAndIsSampleFalseOrderByOrderNumAsc(Long problemId);

    @Modifying
    @Transactional
    void deleteByProblem_ProblemId(Long problemId);
}
