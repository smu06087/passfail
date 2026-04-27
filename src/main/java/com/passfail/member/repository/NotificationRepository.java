package com.passfail.member.repository;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<NotificationEntity> findByMemberOrderByCreatedAtDesc(MemberEntity member);
}
