package com.passfail.payment.repository;

import com.passfail.entity.PaymentEntity;
import com.passfail.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for PaymentEntity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    
    @Query("SELECT SUM(p.amount) FROM PaymentEntity p WHERE p.status = :status")
    Long sumAmountByStatus(@Param("status") PaymentStatus status);
    
    long countByStatus(PaymentStatus status);

    java.util.List<PaymentEntity> findAllByMemberIdAndStatusOrderByPaidAtDesc(Long memberId, PaymentStatus status);
}
