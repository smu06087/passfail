package com.passfail.util.connection;

import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.passfail.entity.MemberEntity;
import com.passfail.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sseapi")
public class SseController {

	private final SseService sseService;

	private final MemberRepository memberRepository;

	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(Authentication authentication) throws Exception {
		
		System.out.println("[SseController] subscribe start");
		if (authentication == null)
		{
			System.out.println("[SseController] subscribe fail, authentication == null");
			throw new Exception();		
		}

		Optional<MemberEntity> optEntity = memberRepository.findByUsername(authentication.getName());

		MemberEntity entity = optEntity.isPresent() ? optEntity.get() : null;

		Long userId = entity != null ? entity.getMemberId() : 0L;

		return sseService.subscribe(userId);
	}
}