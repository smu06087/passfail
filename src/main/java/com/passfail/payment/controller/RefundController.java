package com.passfail.payment.controller;

import com.passfail.entity.PaymentEntity;
import com.passfail.payment.service.KakaoPayService;
import com.passfail.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/payment/refund")
@lombok.extern.slf4j.Slf4j
public class RefundController {

    private final KakaoPayService kakaoPayService;
    private final PaymentService paymentService;

    @GetMapping
    public String refundListPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        List<PaymentEntity> refundablePayments = paymentService.findRefundablePayments(principal.getName());
        model.addAttribute("payments", refundablePayments);
        return "payment/refund-list";
    }

    @PostMapping("/{paymentId}")
    @ResponseBody
    public ResponseEntity<?> refund(@PathVariable("paymentId") Long paymentId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            // 1. DB에서 결제 정보 조회 (검증 포함)
            PaymentEntity payment = paymentService.findRefundablePayments(principal.getName()).stream()
                    .filter(p -> p.getPaymentId().equals(paymentId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("환불 가능한 결제 건을 찾을 수 없습니다."));

            log.info("💳 [환불 시도] TID: {}, DB_Amount: {}", payment.getPgTxnId(), payment.getAmount());

            // 2. 카카오페이 상태 조회 (실제 취소 가능 금액 확인)
            Map<String, Object> orderStatus = kakaoPayService.kakaoPayOrder(payment.getPgTxnId());
            String status = (String) orderStatus.get("status");
            log.info("🔍 [카카오페이 상태 조회] Status: {}, Info: {}", status, orderStatus);

            // 이미 취소된 건이라면 DB만 업데이트 (동기화)
            if ("CANCEL_PAYMENT".equals(status) || "PART_CANCEL_PAYMENT".equals(status)) {
                log.info("⚠️ 이미 취소된 결제 건입니다. DB 동기화를 진행합니다.");
                paymentService.refundPayment(paymentId, principal.getName());
                return ResponseEntity.ok(orderStatus);
            }

            // 취소 가능 금액 확인
            Map<String, Object> cancelAvailable = (Map<String, Object>) orderStatus.get("cancel_available_amount");
            int totalCancelable = (int) cancelAvailable.get("total");

            if (totalCancelable < payment.getAmount()) {
                log.warn("⚠️ 취소 가능 금액({})이 요청 금액({})보다 적습니다.", totalCancelable, payment.getAmount());
                // 취소 가능 금액이 부족하다면, 카카오페이가 허용하는 최대치로 시도하거나 에러 처리
            }

            // 3. 카카오페이 API 호출하여 환불 진행
            Map<String, Object> kakaoResponse = kakaoPayService.kakaoPayCancel(payment.getPgTxnId(), payment.getAmount());
            log.info("✅ [카카오페이 환불 완료] Response: {}", kakaoResponse);

            // 4. 로컬 DB 업데이트 및 포인트 회수
            paymentService.refundPayment(paymentId, principal.getName());

            return ResponseEntity.ok(kakaoResponse);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("❌ [카카오페이 API 오류] Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body("카카오페이 오류: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ [환불 시스템 오류] {}", e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
