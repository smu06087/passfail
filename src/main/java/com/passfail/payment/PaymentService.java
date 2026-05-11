package com.passfail.payment;

import com.passfail.entity.HintUsageEntity;
import com.passfail.entity.MemberEntity;
import com.passfail.entity.PaymentHistory;
import com.passfail.entity.ProblemEntity;
import com.passfail.enums.TXN_Type;
import com.passfail.member.repository.MemberRepository;
import com.passfail.payment.repository.PaymentHistoryRepository;
import com.passfail.problem.repository.HintUsageRepository;
import com.passfail.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MemberRepository memberRepository;
    private final ProblemRepository problemRepository;
    private final HintUsageRepository hintUsageRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    @Transactional
    public String useHint(String username, Long problemId) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        Long memberId = member.getMemberId();

        // 1. 사용자가 이미 해당 문제의 힌트를 구매했는지 확인 (루프 이용)
        List<HintUsageEntity> usages = hintUsageRepository.findByMemberId(memberId);
        for (HintUsageEntity usage : usages) {
            if (usage.getProblemId().equals(problemId)) {
                return getHintText(problemId);
            }
        }

        // 2. 구매 안 했다면 잔액 확인 (500바나나 차감)
        if (member.getPointBalance() < 500) {
            throw new RuntimeException("잔액이 부족합니다. (500 바나나 필요)");
        }

        // 잔액 차감
        member.setPointBalance(member.getPointBalance() - 500);
        memberRepository.save(member);

        // 3. PaymentHistory에 'USE_HINT' 타입으로 이력 저장
        PaymentHistory history = PaymentHistory.builder()
                .memberId(memberId)
                .amount(500L)
                .txnType(TXN_Type.USE_HINT)
                .paymentDate(LocalDateTime.now())
                .build();
        paymentHistoryRepository.save(history);

        // 4. HintUsage에 기록 저장
        HintUsageEntity hintUsage = HintUsageEntity.builder()
                .memberId(memberId)
                .problemId(problemId)
                .build();
        hintUsageRepository.save(hintUsage);

        return getHintText(problemId);
    }

    private String getHintText(Long problemId) {
        ProblemEntity problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));
        return problem.getHint() != null ? problem.getHint() : "이 문제에 대한 힌트가 없습니다.";
    }
}
