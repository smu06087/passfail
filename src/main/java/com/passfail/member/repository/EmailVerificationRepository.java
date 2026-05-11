package com.passfail.member.repository;

import com.passfail.entity.EmailVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 이메일 인증 정보를 관리하는 레포지토리
 */
@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerificationEntity, Long> {
    /**
     * 특정 이메일에 대해 가장 최근에 생성된 인증 정보 조회
     */
    Optional<EmailVerificationEntity> findTopByEmailOrderByCreatedAtDesc(String email);

    /**
     * 이메일과 인증 코드로 인증 정보 조회
     */
    Optional<EmailVerificationEntity> findByEmailAndVerificationCode(String email, String verificationCode);
}
