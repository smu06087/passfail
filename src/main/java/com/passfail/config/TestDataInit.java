package com.passfail.config;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.ProblemEntity;
import com.passfail.entity.TestCaseEntity;
import com.passfail.enums.Difficulty;
import com.passfail.enums.ProblemStatus;
import com.passfail.enums.Role;
import com.passfail.member.repository.MemberRepository;
import com.passfail.problem.repository.ProblemRepository;
import com.passfail.problem.repository.TestCaseRepository;
import com.passfail.problem.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TestDataInit implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final com.passfail.problem.repository.SolvedProblemRepository solvedProblemRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (memberRepository.findByUsername("testuser").isEmpty()) {
            MemberEntity testUser = MemberEntity.builder()
                    .username("testuser")
                    .password(passwordEncoder.encode("password123!"))
                    .email("test@passfail.com")
                    .role(Role.ROLE_USER)
                    .isActive(true)
                    .pointBalance(1000)
                    .totalScore(0)
                    .build();
            memberRepository.save(testUser);
        }

        solvedProblemRepository.deleteAll();
        submissionRepository.deleteAll();
        testCaseRepository.deleteAll();
        problemRepository.deleteAll();

        MemberEntity admin = memberRepository.findByUsername("testuser").orElseThrow();
        
        // 1번 문제
        saveProblemWithTestCases(admin, "두 수의 합", 
            "### 문제 설명\n두 정수 a, b가 주어졌을 때 a + b를 반환하는 프로그램을 작성하세요.\n\n" +
            "### 입력\n한 줄에 두 정수 a, b가 공백으로 구분되어 주어집니다.\n\n" +
            "### 출력\na + b의 결과를 출력하세요.\n\n" +
            "### 입출력 예시\n- **입력:** `3 5` \n- **출력:** `8`", 
            Difficulty.EASY, "Mathematics", 
            List.of(new String[]{"3 5", "8"}, new String[]{"10 20", "30"}));

        // 2번 문제
        saveProblemWithTestCases(admin, "문자열 뒤집기", 
            "### 문제 설명\n주어진 문자열을 거꾸로 뒤집어 반환하는 프로그램을 작성하세요.\n\n" +
            "### 입력\n하나의 문자열이 주어집니다.\n\n" +
            "### 출력\n뒤집힌 문자열을 출력하세요.\n\n" +
            "### 입출력 예시\n- **입력:** `hello` \n- **출력:** `olleh` ", 
            Difficulty.EASY, "String", 
            List.of(new String[]{"hello", "olleh"}, new String[]{"passfail", "liafssap"}));

        // 3번 문제
        saveProblemWithTestCases(admin, "최대값 찾기", 
            "### 문제 설명\n주어진 정수들 중 가장 큰 값을 찾는 프로그램을 작성하세요.\n\n" +
            "### 입력\n첫 줄에 정수의 개수 N이 주어지고, 두 번째 줄에 N개의 정수가 공백으로 구분되어 주어집니다.\n\n" +
            "### 출력\n입력된 정수 중 가장 큰 값을 출력하세요.\n\n" +
            "### 입출력 예시\n- **입력:** \n`5` \n`1 5 3 9 2` \n- **출력:** `9` ", 
            Difficulty.EASY, "Array", 
            List.of(new String[]{"5\n1 5 3 9 2", "9"}, new String[]{"3\n-1 -5 -3", "-1"}));

        System.out.println("✅ 문제 설명 양식 최적화 완료");
    }

    private void saveProblemWithTestCases(MemberEntity admin, String title, String desc, Difficulty diff, String cat, List<String[]> tcs) {
        ProblemEntity p = ProblemEntity.builder()
                .createdBy(admin.getMemberId())
                .title(title)
                .description(desc)
                .difficulty(diff)
                .category(cat)
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .status(ProblemStatus.PUBLISHED)
                .build();
        p = problemRepository.save(p);
        for (String[] tcData : tcs) {
            testCaseRepository.save(TestCaseEntity.builder().problem(p).inputData(tcData[0]).expectedOutput(tcData[1]).isSample(true).build());
        }
    }
}
