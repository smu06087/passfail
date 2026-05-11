package com.passfail.payment.repository;

import com.passfail.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * [Repository - rɪˈpɑːzətɔːri]
 * 비유: "장부를 창고에서 꺼내오거나 새로 적어넣는 창고 관리자 역할을 해요."
 */
@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
}
