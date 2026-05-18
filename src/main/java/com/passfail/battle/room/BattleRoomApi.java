package com.passfail.battle.room;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.passfail.entity.BattleParticipantEntity;
import com.passfail.entity.BattleRoomEntity;

@Controller
@RequestMapping("battle/room/api/")
public class BattleRoomApi {
	
	@Autowired
    private BattleParticipantRepository battleParticipantRepository;
	@Autowired
	private BattleRoomRepository battleRoomRepository;
	
	@Autowired
	private BattleRoomService battleRoomService;
	
	@RequestMapping(value = "leave", method = {RequestMethod.GET, RequestMethod.POST})
	public @ResponseBody String roomLeaveEvent(Long roomId, Long userId) {
		System.out.println("[BattleRoomApi] leave API CALLED - roomId: " + roomId + ", userId: " + userId);
		battleRoomService.leaveRoom(roomId, userId);
		return "success";
	}
}
