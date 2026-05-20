package com.passfail.ai.repository;

import com.passfail.entity.AiChatSessionEntity;
import com.passfail.enums.AiChatHandoffStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AiChatSessionRepository extends JpaRepository<AiChatSessionEntity, Long> {
    Optional<AiChatSessionEntity> findBySessionIdAndMemberId(Long sessionId, Long memberId);
    List<AiChatSessionEntity> findByMemberIdOrderByStartedAtDesc(Long memberId);
    List<AiChatSessionEntity> findBySessionIdInOrderByStartedAtDesc(Collection<Long> sessionIds);
    List<AiChatSessionEntity> findByHandoffStatusAndAssignedAdminIdIsNullOrderByStartedAtDesc(AiChatHandoffStatus status);
    List<AiChatSessionEntity> findByAssignedAdminIdAndHandoffStatusInOrderByStartedAtDesc(Long assignedAdminId, Collection<AiChatHandoffStatus> statuses);

    @Modifying
    @Query("""
        update AiChatSessionEntity s
           set s.assignedAdminId = :adminId,
               s.handoffStatus = :assignedStatus
         where s.sessionId = :sessionId
           and s.handoffStatus = :waitingStatus
           and s.assignedAdminId is null
        """)
    int assignWaitingHandoff(
        @Param("sessionId") Long sessionId,
        @Param("adminId") Long adminId,
        @Param("waitingStatus") AiChatHandoffStatus waitingStatus,
        @Param("assignedStatus") AiChatHandoffStatus assignedStatus
    );
}
