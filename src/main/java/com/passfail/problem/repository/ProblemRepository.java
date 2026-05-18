package com.passfail.problem.repository;

import com.passfail.entity.ProblemEntity;
import com.passfail.enums.Difficulty;
import com.passfail.enums.ProblemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<ProblemEntity, Long> {
    List<ProblemEntity> findByStatus(ProblemStatus status);
    
    @Query("SELECT p FROM ProblemEntity p LEFT JOIN FETCH p.test_cases WHERE p.problemId = :id")
    Optional<ProblemEntity> findByIdWithTestCases(@Param("id") Long id);

	List<ProblemEntity> findByDifficulty(Difficulty difficulty);
}
