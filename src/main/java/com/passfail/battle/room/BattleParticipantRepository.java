package com.passfail.battle.room;


import org.springframework.data.jpa.repository.JpaRepository;

import com.passfail.entity.BattleParticipantEntity;
import java.util.List;




public interface BattleParticipantRepository extends JpaRepository<BattleParticipantEntity, Long> {
    
	@org.springframework.data.jpa.repository.Query("SELECT p FROM BattleParticipantEntity p JOIN FETCH p.member WHERE p.roomId = :roomId")
	List<BattleParticipantEntity> findByRoomIdWithMember(@org.springframework.data.repository.query.Param("roomId") Long roomId);
	
	List<BattleParticipantEntity> findByRoomId(Long roomId);
	
	List<BattleParticipantEntity> findByMemberId(Long memberId);
	
	java.util.Optional<BattleParticipantEntity> findByRoomIdAndMemberId(Long roomId, Long memberId);
	
	void deleteByRoomIdAndMemberId(Long roomId, Long memberId);

	void deleteByRoomId(Long roomId);
}