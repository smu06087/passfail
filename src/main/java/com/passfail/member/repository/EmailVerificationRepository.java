package com.passfail.member.repository;

import com.passfail.entity.EmailVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerificationEntity, Long> {
    Optional<EmailVerificationEntity> findTopByEmailOrderByCreatedAtDesc(String email);
    Optional<EmailVerificationEntity> findByEmailAndVerificationCode(String email, String verificationCode);
}
