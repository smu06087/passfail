package com.passfail.admin.controller;

import com.passfail.admin.service.AdminAnalysisService;
import com.passfail.entity.MemberEntity;
import com.passfail.entity.ProblemTagEntity;
import com.passfail.enums.PaymentStatus;
import com.passfail.enums.Role;
import com.passfail.member.repository.MemberRepository;
import com.passfail.payment.repository.PaymentRepository;
import com.passfail.problem.repository.ProblemRepository;
import com.passfail.problem.repository.ProblemTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
    private final ProblemTagRepository problemTagRepository;
    private final AdminAnalysisService adminAnalysisService;

    // ... (existing analysis and stats methods)

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
            map.put("id", member.getMemberId());
            map.put("name", member.getUsername());
            map.put("email", member.getEmail());
            map.put("tier", member.getTier().name());
            map.put("role", member.getRole().name());
            map.put("date", member.getCreatedAt().toLocalDate().toString());
            map.put("status", member.getIsActive() ? "활성" : "비활성");
            map.put("color", member.getIsActive() ? "text-green-600" : "text-gray-400");
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/members/{id}/role")
    public void updateRole(@PathVariable Long id, @RequestParam Role role) {
        MemberEntity member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        member.setRole(role);
        memberRepository.save(member);
    }

    @GetMapping("/tags")
    public List<String> getTagList() {
        return problemTagRepository.findAll().stream()
                .map(ProblemTagEntity::getTagName)
                .distinct()
                .collect(Collectors.toList());
    }

    @PostMapping("/tags")
    public void addTag(@RequestParam String name) {
        // 실제로는 특정 문제에 연결되어야 하지만, 관리용 마스터 태그 개념으로 첫 번째 문제나 가상의 ID에 연결
        ProblemTagEntity tag = ProblemTagEntity.builder()
                .problemId(1L) // 임시 ID
                .tagName(name)
                .build();
        problemTagRepository.save(tag);
    }

    @DeleteMapping("/tags/{name}")
    public void deleteTag(@PathVariable String name) {
        List<ProblemTagEntity> tags = problemTagRepository.findAll();
        for (ProblemTagEntity tag : tags) {
            if (tag.getTagName().equals(name)) {
                problemTagRepository.delete(tag);
            }
        }
    }

    @GetMapping("/settlements")
    public Map<String, Object> getSettlementData() {
        Long totalRevenue = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        if (totalRevenue == null) totalRevenue = 0L;
        
        double platformFee = 0.3; // 30% 수수료 가정
        long settledAmount = (long) (totalRevenue * (1 - platformFee));
        
        Map<String, Object> data = new HashMap<>();
        data.put("totalRevenue", totalRevenue);
        data.put("platformFee", (long)(totalRevenue * platformFee));
        data.put("settledAmount", settledAmount);
        data.put("period", "2026-05");
        return data;
    }

    @GetMapping("/problems")
    public List<Map<String, Object>> getProblemList() {
        return problemRepository.findAll().stream().map(problem -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", problem.getProblemId());
            map.put("title", problem.getTitle());
            map.put("difficulty", problem.getDifficulty().name());
            map.put("solvedCount", problem.getAcceptedCount());
            map.put("accuracy", Math.round(problem.getAcceptanceRate() * 100) / 100.0);
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/payments")
    public List<Map<String, Object>> getPaymentList() {
        return paymentRepository.findAll().stream().map(payment -> {
            Map<String, Object> map = new HashMap<>();
            map.put("tid", payment.getPgTxnId());
            map.put("username", payment.getMember() != null ? payment.getMember().getUsername() : "Unknown");
            map.put("method", payment.getMethod().name());
            map.put("amount", payment.getAmount());
            map.put("status", payment.getStatus().name());
            map.put("date", payment.getPaidAt().toLocalDate().toString());
            return map;
        }).collect(Collectors.toList());
    }
}
