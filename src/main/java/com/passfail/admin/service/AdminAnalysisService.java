package com.passfail.admin.service;

import com.passfail.entity.PaymentHistory;
import com.passfail.payment.repository.PaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminAnalysisService {

    private final PaymentHistoryRepository paymentHistoryRepository;

    /**
     * [Analysis Logic - 분석 로직]
     * 비유: "흩어져 있는 영수증들을 모아서 월별로 묶고, 사람마다 평균을 내는 계산기예요."
     */
    public Map<String, Object> getPaymentAnalysis() {
        // 1. 모든 결제 데이터를 가져옵니다 (Fetch All [fetʃ ɔːl])
        List<PaymentHistory> historyList = paymentHistoryRepository.findAll();

        // 2. 월별 매출 합계 계산 (Monthly Totals)
        Map<String, Long> monthlyRevenue = new TreeMap<>(); // 정렬을 위해 TreeMap 사용
        
        // 3. 사용자별 결제 금액 목록 (User Payments)
        Map<Long, List<Long>> userPaymentsMap = new HashMap<>();

        // 🔄 [Loop - 반복문] 데이터 전체를 하나씩 돌면서 분석합니다.
        for (PaymentHistory ph : historyList) {
            // (A) 월별 합계 로직
            String month = ph.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            long amount = ph.getAmount();
            
            // 환불(REFUND)인 경우 매출에서 차감 처리
            if ("REFUND".equalsIgnoreCase(ph.getTxnType())) {
                amount = -amount;
            }
            
            monthlyRevenue.put(month, monthlyRevenue.getOrDefault(month, 0L) + amount);

            // (B) 사용자별 목록 수집 로직 (평균 계산 시에는 절대값 또는 별도 처리 필요할 수 있으나 여기서는 단순 합산)
            userPaymentsMap.computeIfAbsent(ph.getMemberId(), k -> new ArrayList<>()).add(amount);
        }

        // 4. 사용자별 평균 결제 금액 계산
        double totalAvg = 0;
        int userCount = userPaymentsMap.size();
        Map<Long, Double> userAverages = new HashMap<>();
        
        for (Map.Entry<Long, List<Long>> entry : userPaymentsMap.entrySet()) {
            List<Long> amounts = entry.getValue();
            long sum = 0;
            for (Long amt : amounts) sum += amt; // 내부 루프로 합계 계산
            
            double avg = (double) sum / amounts.size();
            userAverages.put(entry.getKey(), avg);
            totalAvg += avg;
        }

        // 5. 최종 결과 조립 (Result [rɪˈzʌlt])
        Map<String, Object> result = new HashMap<>();
        result.put("monthlyRevenue", monthlyRevenue);
        result.put("averagePerUser", userCount > 0 ? totalAvg / userCount : 0);
        result.put("rawAverages", userAverages);

        return result;
    }
}
