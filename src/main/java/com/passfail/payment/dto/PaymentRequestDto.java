package com.passfail.payment.dto;

import com.passfail.enums.PaymentMethod;
import com.passfail.enums.PaymentStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {
    private String username;
    private PaymentMethod method;
    private Integer amount;
    private Integer pointCharged;
    private PaymentStatus status;
    private String pgTxnId;
}
