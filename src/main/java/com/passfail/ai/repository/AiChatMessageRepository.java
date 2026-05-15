package com.passfail.ai.repository;

import com.passfail.entity.AiChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessageEntity, Long> {
    List<AiChatMessageEntity> findTop20BySessionIdOrderBySentAtDesc(Long sessionId);
    List<AiChatMessageEntity> findBySessionIdOrderBySentAtAsc(Long sessionId);
    void deleteBySessionId(Long sessionId);
}
