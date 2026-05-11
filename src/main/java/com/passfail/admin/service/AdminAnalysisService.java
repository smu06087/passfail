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
            monthlyRevenue.put(month, monthlyRevenue.getOrDefault(month, 0L) + ph.getAmount());

            // (B) 사용자별 목록 수집 로직
            userPaymentsMap.computeIfAbsent(ph.getMemberId(), k -> new ArrayList<>()).add(ph.getAmount());
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
