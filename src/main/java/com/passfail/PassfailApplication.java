package com.passfail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 🚀 PassFail 메인 애플리케이션
 * 
 * ⭐ @EnableScheduling을 반드시 추가해야
 *    RankingBatchService의 @Scheduled(cron="0 0 0 * * *")
 *    배치가 매일 자정에 실행됩니다!
 */
@SpringBootApplication
@EnableScheduling  // ← ⭐ 이 줄이 반드시 필요!
@org.springframework.scheduling.annotation.EnableAsync
public class PassfailApplication {

    public static void main(String[] args) {
        SpringApplication.run(PassfailApplication.class, args);
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║ 🚀 PassFail 애플리케이션 시작 완료                           ║");
        System.out.println("║ 📊 랭킹 시스템이 활성화되었습니다                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
}