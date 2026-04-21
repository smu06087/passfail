package com.passfail.entity.payment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.passfail.entity.member.MemberEntity;
import com.passfail.enums.TXN_Type;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransactionEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long txnId;

	@Column(name = "member_id",nullable = false)
	private Long memberId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", insertable = false, updatable = false)
	private MemberEntity member;

    @Column(nullable = false, length = 30)
    private TXN_Type txnType;

    @Column(nullable = false)
    private Integer amount;

    @Column(length = 200)
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
