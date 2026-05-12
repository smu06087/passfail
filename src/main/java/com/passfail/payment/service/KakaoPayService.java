package com.passfail.payment.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class KakaoPayService {

    // 🔑 [비밀번호] 카카오페이와 우리만 아는 암호예요. (Secret Key / 시크릿 키)
    @Value("${kakao.api.secret-key}")
    private String secretKey;

    // 🆔 [가맹점 번호] 우리 가게의 이름표 같은 거예요. (CID / 씨아이디)
    @Value("${kakao.api.cid}")
    private String cid;

    // ✅ [성공 통로] 결제가 잘 끝나면 돌아올 길이에요. (Approval URL / 어프루벌 유알엘)
    @Value("${kakao.api.approval-url}")
    private String approvalUrl;

    // ❌ [취소 통로] 결제를 하다가 마음이 바뀌면 돌아올 길이에요. (Cancel URL / 캔슬 유알엘)
    @Value("${kakao.api.cancel-url}")
    private String cancelUrl;

    // ⚠️ [실패 통로] 기계 오류 등으로 결제가 안 되면 돌아올 길이에요. (Fail URL / 페일 유알엘)
    @Value("${kakao.api.fail-url}")
    private String failUrl;

    /**
     * 1단계: "결제 준비할게요!" (kakaoPayReady / 카카오페이 레디)
     * 비유: "사장님, 손님이 바나나 사신대요! 카카오페이에 미리 알려줄게요!"
     */
    public Map<String, Object> kakaoPayReady(String partnerOrderId, String partnerUserId, String itemName, int totalAmount) {
        // 🚀 [우체부] 카카오페이 본사로 편지를 배달해줄 우체부 아저씨예요.
        RestTemplate restTemplate = new RestTemplate();

        // ✉️ [봉투 만들기] 편지 봉투에 '나 우리 가게 사장이야!'라고 도장을 찍어요.
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY " + secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 📝 [편지 내용] 무엇을 팔 건지 상세하게 적어요.
        Map<String, Object> body = new HashMap<>();
        body.put("cid", cid);
        body.put("partner_order_id", partnerOrderId);
        body.put("partner_user_id", partnerUserId);
        body.put("item_name", itemName);
        body.put("quantity", 1);
        body.put("total_amount", totalAmount);
        body.put("vat_amount", 0);
        body.put("tax_free_amount", 0);
        body.put("approval_url", approvalUrl);
        body.put("cancel_url", cancelUrl);
        body.put("fail_url", failUrl);

        // 📦 [택배 박스] 내용물(body)과 도장 찍힌 봉투(headers)를 하나로 합쳐요.
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        // 📍 [주소지] 카카오페이 본사의 주소예요.
        String url = "https://open-api.kakaopay.com/online/v1/payment/ready";

        // 📮 [발송] "똑똑! 결제 준비해주세요!" 하고 편지를 보내고 답장을 받아요.
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        return response.getBody();
    }

    /**
     * 2단계: "최종 승인해주세요!" (kakaoPayApprove / 카카오페이 어프루브)
     * 비유: "손님이 폰으로 확인 누르셨대요! 이제 진짜 돈 옮겨주세요!"
     */
    public Map<String, Object> kakaoPayApprove(String tid, String pgToken, String partnerOrderId, String partnerUserId) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY " + secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 📝 [확인서] 아까 그 결제 건(tid)이 맞는지, 손님이 준 확인권(pgToken)은 있는지 적어요.
        Map<String, Object> body = new HashMap<>();
        body.put("cid", cid);
        body.put("tid", tid);
        body.put("partner_order_id", partnerOrderId);
        body.put("partner_user_id", partnerUserId);
        body.put("pg_token", pgToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        // 📍 [최종 주소] 진짜 결제를 확정 짓는 부서의 주소예요.
        String url = "https://open-api.kakaopay.com/online/v1/payment/approve";

        // 📮 [발송] "이제 진짜 결제 끝!" 하고 답장을 받으면 성공이에요.
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        return response.getBody();
    }
}