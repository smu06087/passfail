package com.passfail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(
	    pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
	) // ← 이걸 추가해야 Pageable 자동 주입 + Page JSON 직렬화 정상 동작
public class PassfailApplication {

	public static void main(String[] args) {
		SpringApplication.run(PassfailApplication.class, args);
	}

}
