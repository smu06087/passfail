package com.passfail.member.repository;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.SocialAccountEntity;
import com.passfail.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 소셜 계정 연동 정보를 관리하는 레포지토리
 */
public interface SocialAccountRepository extends JpaRepository<SocialAccountEntity, Long> {
    /**
     * 서비스 제공자와 해당 서비스의 고유 ID로 연동 정보 조회
     */
    Optional<SocialAccountEntity> findByProviderAndProviderId(Provider provider, String providerId);

    /**
     * 특정 회원과 서비스 제공자로 연동 정보 조회
     */
    Optional<SocialAccountEntity> findByMembersAndProvider(MemberEntity member, Provider provider);
}
