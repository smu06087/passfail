package com.passfail.entity;

import jakarta.persistence.*;
import lombok.*;
import com.passfail.enums.TXN_Type;
import java.time.LocalDateTime;

/**
 * [Entity - ˈentəti]
 * 비유: "은행의 거래 장부 한 줄 한 줄을 컴퓨터가 이해할 수 있는 객체로 만든 거예요."
 */
@Entity
@Table(name = "payment_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 결제한 회원의 번호 (Foreign Key [ˈfɔːrən kiː])
    @Column(nullable = false)
    private Long memberId;

    // 결제 금액 (Amount [əˈmaʊnt])
    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TXN_Type txnType;

    // 결제 일시 (Date [deɪt])
    @Column(nullable = false)
    private LocalDateTime paymentDate;
}
