package com.passfail.ai.service;

import com.passfail.codingtest.dto.ExecutionResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiCodeReviewService {

    public String generateReview(String code, List<ExecutionResult> results) {
        long avgTime = (long) results.stream().mapToLong(ExecutionResult::getExecutionTime).average().orElse(0);
        
        StringBuilder review = new StringBuilder();
        review.append("### AI 코드 리뷰 결과\n\n");
        
        boolean hasNestedLoop = code.contains("for") && code.indexOf("for", code.indexOf("for") + 1) != -1;
        
        if (avgTime > 300) {
            review.append("- ⚠️ **성능 개선 권고**: 평균 실행 시간이 ").append(avgTime).append("ms로 다소 높습니다. ");
            if (hasNestedLoop) {
                review.append("중첩 반복문을 사용하여 시간 복잡도가 O(N^2) 이상일 가능성이 있습니다. 더 효율적인 알고리즘(예: 해시맵, 정렬 후 투포인터 등)을 고려해 보세요.\n");
            } else {
                review.append("불필요한 객체 생성이 나 입출력 연산이 많은지 확인해 보세요.\n");
            }
        } else {
            review.append("- ✅ **우수한 성능**: 실행 시간이 매우 효율적입니다 (평균 ").append(avgTime).append("ms).\n");
        }
        
        if (code.contains("Scanner")) {
            review.append("- 💡 **팁**: 대량의 데이터를 처리할 때는 `Scanner`보다 `BufferedReader`를 사용하는 것이 속도 향상에 도움이 됩니다.\n");
        }
        
        review.append("\n**총평**: 정답을 맞추신 것을 축하드립니다! 가독성과 효율성을 모두 갖춘 코드를 작성하기 위해 계속 노력해 주세요.");
        
        return review.toString();
    }
}
