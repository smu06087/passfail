package com.passfail.ranking.service;

import com.passfail.enums.Difficulty;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 🧮 점수 계산 로직
 * 
 * ─────────────────────────────────────────────────────────────
 * 문제 풀이 기본 점수:
 *   - EASY:   10점  (쉬운 문제)
 *   - MEDIUM: 30점  (중간 문제)
 *   - HARD:   70점  (어려운 문제)
 * 
 * 향후 추가 가능:
 *   - 첫 풀이 보너스 (10%)
 *   - 연속 풀이 보너스 (5%)
 *   - 시간 단축 보너스
 * ─────────────────────────────────────────────────────────────
 */
@Component
@NoArgsConstructor
public class ScoreCalculator {

    /**
     * 난이도에 따른 기본 점수 계산
     * 
     * @param difficulty 문제 난이도 (EASY, MEDIUM, HARD)
     * @return 획득 점수
     */
    public Integer calculateProblemScore(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 10;    // 쉬운 문제: 10점
            case MEDIUM:
                return 30;    // 중간 문제: 30점
            case HARD:
                return 70;    // 어려운 문제: 70점
            default:
                return 0;
        }
    }

    /**
     * 🎮 게임 결과에 따른 점수 계산 (향후 사용)
     * 
     * 로직:
     *   - 승리: 기본 점수 × 2
     *   - 패배: 기본 점수 × 0.5
     * 
     * @param isWin 게임 승패 (true = 승리, false = 패배)
     * @param baseScore 게임 기본 점수
     * @return 최종 게임 점수
     */
    public Integer calculateGameScore(Boolean isWin, Integer baseScore) {
        if (isWin) {
            return baseScore * 2;  // 승리: 2배
        } else {
            return (int)(baseScore * 0.5);  // 패배: 0.5배
        }
    }

    /**
     * 보너스 포함 최종 점수 계산 (향후 확장)
     * 
     * @param baseScore 기본 점수
     * @param isFirstSolve 첫 풀이 여부
     * @param solveStreak 연속 풀이 횟수
     * @return 보너스 포함 최종 점수
     */
    public Integer calculateWithBonus(Integer baseScore, Boolean isFirstSolve, Integer solveStreak) {
        Integer finalScore = baseScore;
        
        // 첫 풀이 시 10% 보너스
        if (Boolean.TRUE.equals(isFirstSolve)) {
            finalScore = (int)(finalScore * 1.1);
        }
        
        // 연속 풀이 5회 이상 시 5% 추가 보너스
        if (solveStreak != null && solveStreak >= 5) {
            finalScore = (int)(finalScore * 1.05);
        }
        
        return finalScore;
    }
}