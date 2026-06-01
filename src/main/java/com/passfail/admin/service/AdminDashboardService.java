package com.passfail.admin.service;

import com.passfail.admin.dto.AdminDashboardDto;
import com.passfail.admin.dto.MemberStatusDto;
import com.passfail.admin.dto.PaymentSummaryDto;
import com.passfail.enums.PaymentStatus;
import com.passfail.member.repository.MemberRepository;
import com.passfail.payment.repository.PaymentRepository;
import com.passfail.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final MemberRepository memberRepository;
    private final ProblemRepository problemRepository;
    private final PaymentRepository paymentRepository;
    private final VisitorService visitorService;

    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboardData() {
        // 1. 핵심 요약 데이터 (Summary Data)
        long totalMembers = memberRepository.count();
        long totalProblems = problemRepository.count();
        Long totalRevenue = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        if (totalRevenue == null) totalRevenue = 0L;
        
        long todayVisits = visitorService.getTodayVisitorCount();

        // 2. 회원 상태 분포 (Member Status Distribution)
        MemberStatusDto memberStatus = MemberStatusDto.builder()
                .activeCount(memberRepository.countByIsActiveTrue())
                .inactiveCount(memberRepository.countByIsActiveFalse())
                .suspendedCount(0) // 추가적인 상태 엔티티 속성이 필요함
                .withdrawnCount(0) // 추가적인 상태 엔티티 속성이 필요함
                .build();

        // 3. 매출 요약 (Payment Summaries)
        Long successAmount = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        Long refundAmount = paymentRepository.sumAmountByStatus(PaymentStatus.REFUNDED);
        if (successAmount == null) successAmount = 0L;
        if (refundAmount == null) refundAmount = 0L;

        PaymentSummaryDto success = PaymentSummaryDto.builder()
                .label("결제 완료")
                .value(successAmount)
                .color("text-green-600")
                .build();
        
        PaymentSummaryDto refundRequested = PaymentSummaryDto.builder()
                .label("환불 요청")
                .value(0) // 현재 즉시 환불 처리되므로 요청 중 상태는 0으로 유지 (향후 필요 시 로직 추가)
                .color("text-amber-500")
                .build();

        PaymentSummaryDto refunded = PaymentSummaryDto.builder()
                .label("환불 완료")
                .value(refundAmount)
                .color("text-gray-400")
                .build();

        return AdminDashboardDto.builder()
                .totalMembers(totalMembers)
                .totalProblems(totalProblems)
                .totalRevenue(totalRevenue)
                .todayVisits(todayVisits)
                .memberStatus(memberStatus)
                .paymentSummaries(Arrays.asList(success, refunded))
                .build();
    }
}
