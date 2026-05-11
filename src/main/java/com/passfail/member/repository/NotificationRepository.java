package com.passfail.member.repository;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 사용자 알림 정보를 관리하는 레포지토리
 */
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    /**
     * 회원 ID로 알림 목록 조회 (최신순)
     */
    List<NotificationEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 회원 엔티티로 알림 목록 조회 (최신순)
     */
    List<NotificationEntity> findByMemberOrderByCreatedAtDesc(MemberEntity member);
}
