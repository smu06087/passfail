package com.passfail.member.repository;

import com.passfail.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 회원(Member) 엔티티에 대한 데이터 액세스 처리를 담당하는 레포지토리
 */
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    /**
     * 사용자 닉네임(ID)으로 회원 정보 조회
     */
    Optional<MemberEntity> findByUsername(String username);

    /**
     * 이메일 주소로 회원 정보 조회
     */
    Optional<MemberEntity> findByEmail(String email);
}
