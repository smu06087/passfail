package com.passfail.admin.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSummaryDto {
    private String label;
    private long value;
    private String color;
}
