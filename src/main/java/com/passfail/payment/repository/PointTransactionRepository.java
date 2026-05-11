package com.passfail.payment.repository;

import com.passfail.entity.PointTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransactionEntity, Long> {
}
