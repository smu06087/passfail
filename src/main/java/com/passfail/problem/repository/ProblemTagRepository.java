package com.passfail.problem.repository;

import com.passfail.entity.ProblemTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface ProblemTagRepository extends JpaRepository<ProblemTagEntity, Long> {
    List<ProblemTagEntity> findByProblemIdOrderByTagIdAsc(Long problemId);
    
    @Modifying
    @Transactional
    void deleteByProblemId(Long problemId);
}
