package com.passfail.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.passfail.entity.MemberEntity;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    Optional<MemberEntity> findByUsername(String username);

    Optional<MemberEntity> findByEmail(String email);

    long countByIsActiveTrue();

    long countByIsActiveFalse();

    Optional<MemberEntity> findTopByOrderByMemberIdAsc();
}
