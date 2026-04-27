package com.passfail.member.repository;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.TermsConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TermsConsentRepository extends JpaRepository<TermsConsentEntity, Long> {
    Optional<TermsConsentEntity> findByMember(MemberEntity member);
}
