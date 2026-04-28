package com.passfail.config;

import com.passfail.entity.MemberEntity;
import com.passfail.enums.Role;
import com.passfail.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TestDataInit implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 시스템 초기 접근을 위한 테스트 유저 생성 (존재하지 않을 때만)
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
            System.out.println("✅ 초기 테스트 유저(testuser) 생성 완료");
        }
        
        // 문제 자동 생성 및 삭제 로직은 사용자가 직접 관리하기 위해 제거함
        System.out.println("🚀 시스템 준비 완료 (문제 데이터는 DB 설정을 유지합니다)");
    }
}
