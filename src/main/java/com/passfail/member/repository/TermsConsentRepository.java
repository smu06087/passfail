package com.passfail.member.repository;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.TermsConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 약관 동의 정보를 관리하는 레포지토리
 */
@Repository
public interface TermsConsentRepository extends JpaRepository<TermsConsentEntity, Long> {
    /**
     * 특정 회원의 약관 동의 내역 조회
     */
    Optional<TermsConsentEntity> findByMember(MemberEntity member);
}
