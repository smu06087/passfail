package com.passfail.ranking.service;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.TotalTierEntity;
import com.passfail.enums.Difficulty;
import com.passfail.enums.Tier;
import com.passfail.member.repository.MemberRepository;
import com.passfail.ranking.repository.TotalTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 🎯 실시간 점수 반영 서비스 (PK 구조 변경 반영 완료)
 */
@Service
@RequiredArgsConstructor
public class RealTimeScoreService {

    private final TotalTierRepository totalTierRepository;
    private final MemberRepository memberRepository;
    private final ScoreCalculator scoreCalculator;

    /**
     * 📝 문제 풀이 후 점수를 실시간으로 반영
     */
    @Transactional
    public void addScoreFromProblem(Long memberId, Difficulty difficulty) {
        System.out.println("📝 [실시간] 점수 반영 시작: memberId=" + memberId);
        
        Integer addScore = scoreCalculator.calculateProblemScore(difficulty);

        // 🌟 수정: findById 대신 findByMember_MemberId 사용
        TotalTierEntity totalTier = totalTierRepository.findByMember_MemberId(memberId)
                .orElseGet(() -> createInitialTotalTier(memberId));

        totalTier.addProblemScore(addScore);
        System.out.println("   ✅ 반영 후 점수: " + totalTier.getTotalScore());
    }

    /**
     * 🎮 게임 결과 후 점수를 실시간으로 반영
     */
    @Transactional
    public void addScoreFromGame(Long memberId, Boolean isWin, Integer baseGameScore) {
        System.out.println("🎮 [실시간] 게임 점수 반영: memberId=" + memberId);

        Integer gameScore = scoreCalculator.calculateGameScore(isWin, baseGameScore);

        // 🌟 수정: findById 대신 findByMember_MemberId 사용
        TotalTierEntity totalTier = totalTierRepository.findByMember_MemberId(memberId)
                .orElseGet(() -> createInitialTotalTier(memberId));

        totalTier.addGameScore(gameScore);
        System.out.println("   ✅ 반영 후 점수: " + totalTier.getTotalScore());
    }

    /**
     * 🛠 내부 메서드: 초기 랭킹 데이터 생성 (PK 자동 생성 방식 반영)
     */
    private TotalTierEntity createInitialTotalTier(Long memberId) {
        System.out.println("   ⚠️ 데이터 신규 생성 (memberId: " + memberId + ")");
        
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 없음: " + memberId));

        // 🌟 수정: memberId 필드를 직접 세팅하지 않고 연관관계(member)만 설정
        // PK인 totalTierId는 @GeneratedValue에 의해 DB 시퀀스가 처리합니다.
        TotalTierEntity newTier = TotalTierEntity.builder()
                .member(member)
                .totalScore(0)
                .currentRank(999999)
                .tier(Tier.BRONZE)
                .lastUpdatedAt(LocalDateTime.now())
                .build();

        return totalTierRepository.save(newTier);
    }
}
