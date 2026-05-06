package com.passfail.ranking.controller;

import com.passfail.ranking.dto.RankingResponseDTO;
import com.passfail.ranking.service.RankingInitBatchService;
import com.passfail.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🏆 랭킹 게시판 컨트롤러
 */
@Controller
@RequestMapping("/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;
    private final RankingInitBatchService rankingInitBatchService;

    /**
     * 1️⃣ 랭킹 게시판 페이지
     */
    @GetMapping
    public String rankingPage() {
        return "ranking/list";
    }

    /**
     * 2️⃣ 전체 랭킹 데이터 (JSON)
     */
    @GetMapping("/top")
    @ResponseBody
    public List<RankingResponseDTO> getTopRankings() {
        return rankingService.getTopRankings();
    }

    /**
     * 3️⃣ 개별 유저 랭킹 정보 (JSON)
     */
    @GetMapping("/me/{memberId}")
    @ResponseBody
    public RankingResponseDTO getMyRanking(@PathVariable Long memberId) {
        return rankingService.getMyRanking(memberId);
    }

    /**
     * 4️⃣ 배치 수동 실행 (관리자 전용)
     */
    @PostMapping("/update")
    @ResponseBody
    public String forceUpdateRankings() {
        rankingService.refreshDailyRanking();
        return "✅ 랭킹이 성공적으로 업데이트되었습니다.";
    }

    /**
     * 5️⃣ ⭐ 초기화 배치 (기존 회원 데이터 마이그레이션)
     * ─────────────────────────────────────────────────────────────
     * 접속 URL: /ranking/init-batch (GET 또는 POST)
     * 권한: 관리자만 (선택사항)
     * 역할: 기존 회원의 totalScore를 TotalTierEntity로 복사
     * 
     * ⚠️ 주의: 처음 한 번만 실행! (이후는 자동 배치가 담당)
     */
    @GetMapping("/init-batch")
    @ResponseBody
    // @PreAuthorize("hasRole('ADMIN')") // ← 원하면 관리자 인증 추가
    public String initializeBatch() {
        System.out.println("\n🚀 [요청] /ranking/init-batch 초기화 배치 시작\n");
        rankingInitBatchService.initializeRankingForAllMembers();
        return "✅ 초기화 배치 완료! /ranking에서 확인하세요.";
    }
}