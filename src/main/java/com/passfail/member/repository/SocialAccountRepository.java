package com.passfail.member.repository;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.SocialAccountEntity;
import com.passfail.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccountEntity, Long> {
    Optional<SocialAccountEntity> findByProviderAndProviderId(Provider provider, String providerId);
    Optional<SocialAccountEntity> findByMembersAndProvider(MemberEntity member, Provider provider);
}
