package com.passfail.problem.repository;

import com.passfail.entity.TestCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCaseEntity, Long> {
    @Query("SELECT tc FROM TestCaseEntity tc WHERE tc.problem.problemId = :problemId AND tc.isSample = true ORDER BY tc.orderNum Asc")
    List<TestCaseEntity> findByProblemIdAndIsSampleTrueOrderByOrderNumAsc(Long problemId);

    @Query("SELECT tc FROM TestCaseEntity tc WHERE tc.problem.problemId = :problemId AND tc.isSample = false ORDER BY tc.orderNum Asc")
    List<TestCaseEntity> findByProblemIdAndIsSampleFalseOrderByOrderNumAsc(Long problemId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TestCaseEntity tc WHERE tc.problem.problemId = :problemId")
    void deleteByProblemId(Long problemId);
}
