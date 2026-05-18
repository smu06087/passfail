package com.passfail.battle.room;

import java.time.LocalDateTime;

import com.passfail.entity.BattleRoomEntity;
import com.passfail.enums.BattleRoomStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleRoomDTO {

	private Long roomId;

	private Long hostId;
	
	private String roomName;
	
	private String difficulty;
	
	private boolean hasPassword;
	
	private int currentParticipantsCount;
	
	private Long problemId;	

    private com.passfail.enums.BattleMode battleMode;

    private BattleRoomStatus status;

    private Integer maxParticipants;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String tags;

    private LocalDateTime createdAt;   

}
