package com.passfail.payment;

import java.util.Map;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/order") // 🏠 [가게 주소] 우리 가게의 '주문 센터' 주소예요. (Order / 오더)
public class OrderController {
	
    @Autowired
    private KakaoPayService kakaoPayService; // 🤵 [비서 호출] 결제 전문 비서님을 불러왔어요.

    /**
     * 1. 결제 준비 벨 누르기 (kakaoPayReady / 카카오페이 레디)
     * 비유: 손님이 "이거 살게요!" 하고 결제 버튼을 꾹 누른 상황이에요.
     */
    @PostMapping("/kakaoPayReady")
    public ResponseEntity<?> kakaoPayReady(HttpSession session) {
        try {
            // 📞 [비서에게 요청] "비서님, 카카오페이에 결제 준비해달라고 말해주세요!"
            Map<String, Object> response = kakaoPayService.kakaoPayReady();
            
            // 🔖 [번호표 기억] 결제 번호(TID)가 적힌 번호표를 세션(장바구니)에 잠깐 넣어둬요.
            // 그래야 나중에 "아까 그 손님 맞네!" 하고 알 수 있거든요.
            session.setAttribute("tid", response.get("tid")); 
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 😵 [비상 상황] 혹시라도 카카오페이가 "바빠요!"라고 하면 손님에게 이유를 알려줘요.
            Map<String, String> errorMap = new HashMap<>();
            String[] errKeys = {"error", "message"};
            String[] errVals = {"결제 준비 실패", e.getMessage()};
            
            for(int i=0; i<errKeys.length; i++) { // 🔁 [반복문] 에러 내용을 담아요.
                errorMap.put(errKeys[i], errVals[i]);
            }
            return ResponseEntity.internalServerError().body(errorMap);
        }
    }

    /**
     * 2. 결제 성공 도장 쾅! (kakaoPaySuccess / 카카오페이 석세스)
     * 비유: 손님이 자기 폰으로 결제 승인을 마쳤을 때 호출되는 단계예요.
     */
    @GetMapping("/success")
    public String kakaoPaySuccess(@RequestParam("pg_token") String pgToken, HttpSession session, Model model) {
        // 🔍 [번호표 찾기] 아까 세션(장바구니)에 넣어둔 번호표(TID)를 꺼내요.
        String tid = (String) session.getAttribute("tid");
        
        // 🚫 [번호표 분실] 번호표가 없으면 "누구신지 모르겠어요!" 하고 실패 화면으로 보내요.
        if (tid == null) {
            return "redirect:/pay/fail"; 
        }

        try {
            // ✅ [최종 확인] 비서에게 번호표(TID)와 손님이 가져온 확인 딱지(pgToken)를 줘요.
            Map<String, Object> response = kakaoPayService.kakaoPayApprove(tid, pgToken);
            
            // 📄 [영수증 준비] 결제가 잘 됐으니 화면(HTML)에 보여줄 정보를 모델에 담아요.
            model.addAttribute("info", response);
            
            return "success"; // 🎉 [성공 화면] success.html(영수증 화면)을 보여줘요!
            
        } catch (Exception e) {
            // 😭 [결제 실패] 돈이 부족하거나 다른 이유로 실패하면 에러 페이지로 가요.
            model.addAttribute("error", e.getMessage());
            return "error"; 
        }
    }
    
    /**
     * 3. 주문서 작성 화면 (payForm / 페이 폼)
     * 비유: 손님에게 "무엇을 살지 적어주세요" 하고 종이를 건네는 거예요.
     */
    @GetMapping("/pay/form")
    public String pay () {
        return "orderform"; // 📝 주문 양식(orderform.html)으로 이동!
    }
    
    /**
     * 4. 완료 확인 (completed / 컴플리티드)
     */
    @GetMapping("/completed")
    public String pa () {
        return "orderform"; // (참고: 여기는 보통 결제 완료 페이지로 연결해야 해요!)
    }
}