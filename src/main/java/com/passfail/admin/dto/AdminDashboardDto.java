package com.passfail.admin.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardDto {
    private long totalMembers;
    private long totalProblems;
    private long totalRevenue;
    private long todayVisits; // Mocked for now if no visit log exists
    
    private MemberStatusDto memberStatus;
    private List<PaymentSummaryDto> paymentSummaries;
}
