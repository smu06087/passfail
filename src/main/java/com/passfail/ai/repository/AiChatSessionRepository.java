package com.passfail.ai.repository;

import com.passfail.entity.AiChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AiChatSessionRepository extends JpaRepository<AiChatSessionEntity, Long> {
    Optional<AiChatSessionEntity> findBySessionIdAndMemberId(Long sessionId, Long memberId);
    List<AiChatSessionEntity> findByMemberIdOrderByStartedAtDesc(Long memberId);
    List<AiChatSessionEntity> findBySessionIdInOrderByStartedAtDesc(Collection<Long> sessionIds);
}
