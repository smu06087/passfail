package com.passfail.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.ArrayList;
import java.util.List;

@Controller
public class adminController {

    /**
     * 관리자 페이지 연결 (GET 방식)
     * 발음: 겟 매핑 [겟 매핑]
     */
    @GetMapping("/admin")
    public String adminPage() {
        // 반복문을 사용하여 콘솔에 접속 로그를 5번 출력하는 예시
        // 발음: 포 루프 [포 루프]
        for (int i = 1; i <= 5; i++) {
            System.out.println("관리자 페이지에 " + i + "번째 접속 시도 중...");
        }
        
        return "admin"; // admin.html 파일을 찾아감
    }
}