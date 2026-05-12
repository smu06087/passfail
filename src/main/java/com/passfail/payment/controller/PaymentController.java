package com.passfail.payment.controller;

import com.passfail.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/payment")
    public String paymentPage() {
        return "payment/payment";
    }

    @PostMapping("/api/hint/{problemId}")
    @ResponseBody
    public String useHint(@PathVariable Long problemId, Principal principal) {
        if (principal == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        return paymentService.useHint(principal.getName(), problemId);
    }

    @GetMapping("/payment/success")
    public String success() {
        return "payment/success";
    }

    @GetMapping("/payment/cancel")
    public String cancel() {
        return "index";
    }

    @GetMapping("/payment/fail")
    public String fail() {
        return "error/404";
    }
}
