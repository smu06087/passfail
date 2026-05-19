package com.passfail.battle.room;

import com.passfail.entity.BattleRogueProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface BattleRogueProgressRepository extends JpaRepository<BattleRogueProgressEntity, Long> {
    Optional<BattleRogueProgressEntity> findByRoomIdAndMemberId(Long roomId, Long memberId);
    List<BattleRogueProgressEntity> findByRoomId(Long roomId);
    void deleteByRoomId(Long roomId);
}
