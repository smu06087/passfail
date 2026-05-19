package com.passfail.admin.controller;

import com.passfail.admin.dto.AdminDashboardDto;
import com.passfail.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * 내용 : Passfail 서비스의 관리자 페이지(Admin Dashboard)를 제어하고 
 * 데이터 상태를 뷰(View)로 전달하기 위한 Controller 클래스
 */
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    /*
     * 기능 : 관리자 메인 대시보드 연결 및 실제 DB 데이터 전달
     * 매개변수(Param) : Model - 화면에 데이터를 전달하기 위한 객체
     * 반환값(Return) : String - 이동할 HTML 파일명 (admin)
     */
    @GetMapping("/admin")
    public String adminPage(Model model) {
        // 실제 DB 데이터를 DTO를 통해 가져옵니다.
        AdminDashboardDto dashboardData = adminDashboardService.getDashboardData();
        
        // 화면에 보여줄 데이터를 모델에 담습니다.
        model.addAttribute("data", dashboardData);

        return "admin";    }
}