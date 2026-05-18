package com.passfail.battle.room;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.passfail.entity.BattleRoomEntity;
import com.passfail.enums.BattleRoomStatus;


public interface BattleRoomRepository extends JpaRepository<BattleRoomEntity, Long> {
    
	@Query(value = "SELECT r FROM BattleRoomEntity r " +
            "WHERE r.status = 'WAITING' " +
            "AND EXISTS (SELECT 1 FROM BattleParticipantEntity p WHERE p.roomId = r.roomId) " +
            "AND (SELECT COUNT(p2.bpId) FROM BattleParticipantEntity p2 WHERE p2.roomId = r.roomId) <= r.maxParticipants",
           countQuery = "SELECT COUNT(r) FROM BattleRoomEntity r WHERE r.status = 'WAITING' AND EXISTS (SELECT 1 FROM BattleParticipantEntity p WHERE p.roomId = r.roomId)")
	Page<BattleRoomEntity> findEnterableRooms(Pageable pageable);
	
	@Query(value = "SELECT r FROM BattleRoomEntity r " +
            "WHERE r.status = :status " +
            "AND EXISTS (SELECT 1 FROM BattleParticipantEntity p WHERE p.roomId = r.roomId)",
           countQuery = "SELECT COUNT(r) FROM BattleRoomEntity r WHERE r.status = :status AND EXISTS (SELECT 1 FROM BattleParticipantEntity p WHERE p.roomId = r.roomId)")
	Page<BattleRoomEntity> findByStatusWithParticipants(BattleRoomStatus status, Pageable pageable);
    
	@Query(value = "SELECT r FROM BattleRoomEntity r " +
            "WHERE EXISTS (SELECT 1 FROM BattleParticipantEntity p WHERE p.roomId = r.roomId)",
           countQuery = "SELECT COUNT(r) FROM BattleRoomEntity r WHERE EXISTS (SELECT 1 FROM BattleParticipantEntity p WHERE p.roomId = r.roomId)")
    Page<BattleRoomEntity> findAllWithParticipants(Pageable pageable);
    
    Page<BattleRoomEntity> findByStatus(BattleRoomStatus status, Pageable pageable);
    
    Page<BattleRoomEntity> findAll(Pageable pageable);
    
    @Query("SELECT r FROM BattleRoomEntity r WHERE r.status = 'WAITING' AND (r.password IS NULL OR r.password = '') AND SIZE(r.participants) > 0 AND SIZE(r.participants) < r.maxParticipants")
    List<BattleRoomEntity> findQuickMatchCandidates();

    @Query("SELECT r FROM BattleRoomEntity r WHERE " +
           "r.status <> 'FINISHED' AND " +
           "SIZE(r.participants) > 0 AND " +
           "(:enterableOnly = false OR (r.status = 'WAITING' AND SIZE(r.participants) < r.maxParticipants)) AND " +
           "(:query IS NULL OR :query = '' OR " +
           "CAST(r.roomId AS string) LIKE %:query% OR " +
           "r.roomName LIKE %:query% OR " +
           "r.tags LIKE %:query% OR " +
           "CAST(r.difficulty AS string) LIKE %:query%) AND " +
           "(:tag IS NULL OR :tag = '' OR r.tags LIKE %:tag% OR CAST(r.difficulty AS string) LIKE %:tag%)")
    Page<BattleRoomEntity> searchRooms(@org.springframework.data.repository.query.Param("query") String query, 
                                       @org.springframework.data.repository.query.Param("tag") String tag, 
                                       @org.springframework.data.repository.query.Param("enterableOnly") boolean enterableOnly, 
                                       Pageable pageable);
}