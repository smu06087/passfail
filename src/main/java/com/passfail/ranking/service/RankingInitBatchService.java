package com.passfail.ranking.service;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.TotalTierEntity;
import com.passfail.member.repository.MemberRepository;
import com.passfail.ranking.repository.TotalTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingInitBatchService {

    private final MemberRepository memberRepository;
    private final TotalTierRepository totalTierRepository;
    private final TransactionTemplate transactionTemplate;

    public void initializeRankingForAllMembers() {
        List<MemberEntity> allMembers = memberRepository.findAll();
        
        for (MemberEntity member : allMembers) {
            transactionTemplate.execute(status -> {
                try {
                    TotalTierEntity tier = totalTierRepository.findByMember_MemberId(member.getMemberId())
                            .orElseGet(() -> TotalTierEntity.builder()
                                    .member(member)
                                    .currentRank(999999)
                                    .build());

                    tier.setTotalScore(member.getTotalScore() != null ? member.getTotalScore() : 0);
                    totalTierRepository.save(tier);
                    System.out.println("✅ 성공: " + member.getUsername());
                } catch (Exception e) {
                    System.out.println("❌ 실패: " + member.getUsername() + " - " + e.getMessage());
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }
}
