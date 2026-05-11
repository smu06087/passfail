package com.passfail.ranking.controller;

import com.passfail.ranking.dto.RankingResponseDTO;
import com.passfail.ranking.service.RankingInitBatchService;
import com.passfail.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🏆 랭킹 게시판 컨트롤러
 * 
 * ⭐ 모든 @PathVariable, @RequestParam에 명시적으로 name 지정
 * (컴파일 설정 없이도 작동하도록)
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
     * 2️⃣ 전체 랭킹 데이터 (기본 정보)
     */
    @GetMapping("/top")
    @ResponseBody
    public List<RankingResponseDTO> getTopRankings() {
        return rankingService.getTopRankings();
    }

    /**
     * 3️⃣ 개별 유저 랭킹 정보
     * ⭐ 수정: @PathVariable에 name="memberId" 명시적 지정
     */
    @GetMapping("/me/{memberId}")
    @ResponseBody
    public RankingResponseDTO getMyRanking(
            @PathVariable(name = "memberId") Long memberId) {
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
     * 5️⃣ 초기화 배치 (기존 회원 데이터 마이그레이션)
     */
    @GetMapping("/init-batch")
    @ResponseBody
    public String initializeBatch() {
        System.out.println("\n🚀 [요청] /ranking/init-batch 초기화 배치 시작\n");
        rankingInitBatchService.initializeRankingForAllMembers();
        return "✅ 초기화 배치 완료! /ranking에서 확인하세요.";
    }
}