package com.passfail.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.ArrayList;
import java.util.List;
/*
 * 작성자 : 신동엽
 * 내용 : Passfail 서비스의 관리자 페이지(Admin Dashboard)를 제어하고 
 * 데이터 상태를 뷰(View)로 전달하기 위한 Controller 클래스
 */
@Controller
public class adminController {
    /*
     * 기능 : 관리자 메인 대시보드 연결 및 상태 로그 출력
     * 매개변수(Param) : Model - 화면에 데이터를 전달하기 위한 객체
     * 반환값(Return) : String - 이동할 HTML 파일명 (admin)
     */
    @GetMapping("/admin")
    public String adminPage(Model model) {
        // 루프(Loop [luːp]) 구조를 사용하여 접속 정보를 반복 기록합니다.
        String[] adminLogs = {
            "보안 세션 체크 완료",
            "관리자 권한 확인됨",
            "통계 데이터 로딩 시작",
            "시스템 리소스 점검",
            "최종 렌더링 준비"
        };
        // 이터레이션(Iteration [ˌɪtəˈreɪʃn])을 통한 로그 처리
        for (int i = 0; i < adminLogs.length; i++) {
            System.out.println("[ADMIN-LOG] Step " + (i + 1) + ": " + adminLogs[i]);
        }
        // 화면에 보여줄 작성자 정보를 모델에 담습니다.
        model.addAttribute("author", "신동엽");
        return "admin"; 
    }
}