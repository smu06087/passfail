package com.passfail.battle.room;

import org.springframework.data.jpa.repository.JpaRepository;
import com.passfail.entity.BattleCompetitionPermissionEntity;
import java.util.Optional;

public interface BattleCompetitionPermissionRepository extends JpaRepository<BattleCompetitionPermissionEntity, Long> {
    Optional<BattleCompetitionPermissionEntity> findByMemberId(Long memberId);
}
