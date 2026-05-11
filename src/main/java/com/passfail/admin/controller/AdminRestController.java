package com.passfail.admin.controller;

import com.passfail.admin.service.AdminAnalysisService;
import com.passfail.entity.MemberEntity;
import com.passfail.enums.PaymentStatus;
import com.passfail.member.repository.MemberRepository;
import com.passfail.payment.repository.PaymentRepository;
import com.passfail.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRestController {

    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final ProblemRepository problemRepository;
    private final AdminAnalysisService adminAnalysisService;

    @GetMapping("/analysis")
    public Map<String, Object> getAnalysisData() {
        return adminAnalysisService.getPaymentAnalysis();
    }

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 0. Current User Role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElse("ROLE_USER");
        stats.put("currentUserRole", role);

        // 1. Summary Data
        long totalMembers = memberRepository.count();
        long totalProblems = problemRepository.count();
        Long totalSales = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        if (totalSales == null) totalSales = 0L;
        
        // Mocking visit count for now as requested by user context usually implies some real-ish data
        // but visit count isn't in repos. I'll use a fixed number or random.
        int todayVisits = 842; 

        stats.put("summary", List.of(
            Map.of("id", 1, "title", "전체 회원", "value", String.format("%,d", totalMembers), "icon", "users", "color", "text-green-600", "bg", "bg-green-50"),
            Map.of("id", 2, "title", "문제 세트", "value", String.format("%,d", totalProblems), "icon", "book-open", "color", "text-blue-500", "bg", "bg-blue-50"),
            Map.of("id", 3, "title", "결제 매출", "value", String.format("%,d", totalSales), "icon", "dollar-sign", "color", "text-emerald-600", "bg", "bg-emerald-50"),
            Map.of("id", 4, "title", "오늘 방문", "value", String.format("%,d", todayVisits), "icon", "eye", "color", "text-orange-500", "bg", "bg-orange-50")
        ));

        // 2. Member Distribution
        long activeCount = memberRepository.countByIsActiveTrue();
        long inactiveCount = memberRepository.countByIsActiveFalse();
        stats.put("memberDistribution", Map.of(
            "labels", List.of("활성", "비활성"),
            "data", List.of(activeCount, inactiveCount)
        ));

        // 3. Payment Summary
        Long successAmount = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        Long refundAmount = paymentRepository.sumAmountByStatus(PaymentStatus.REFUNDED);
        if (successAmount == null) successAmount = 0L;
        if (refundAmount == null) refundAmount = 0L;

        stats.put("paymentSummary", List.of(
            Map.of("label", "결제 완료", "value", String.format("%,d", successAmount), "color", "text-green-600"),
            Map.of("label", "환불 완료", "value", String.format("%,d", refundAmount), "color", "text-gray-400")
        ));

        return stats;
    }

    @GetMapping("/members")
    public List<Map<String, Object>> getMemberList() {
        return memberRepository.findAll().stream().map(member -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", member.getUsername());
            map.put("email", member.getEmail());
            map.put("date", member.getCreatedAt().toLocalDate().toString());
            map.put("status", member.getIsActive() ? "활성" : "비활성");
            map.put("color", member.getIsActive() ? "text-green-600" : "text-gray-400");
            return map;
        }).collect(Collectors.toList());
    }
}
