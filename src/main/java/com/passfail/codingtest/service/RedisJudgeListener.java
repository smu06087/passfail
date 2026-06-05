package com.passfail.codingtest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passfail.codingtest.dto.CustomTestCaseRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis Queue를 모니터링하여 채점 및 실행 작업을 수행하는 리스너
 * JSON 메시지를 파싱하여 제출(SUBMIT)과 실행(RUN)을 구분합니다.
 * 리눅스(배포 환경)에서만 동작하도록 설정되었습니다.
 */
@Service
@Slf4j
@ConditionalOnExpression("#{systemProperties['os.name'].toLowerCase().contains('linux')}")
public class RedisJudgeListener {

    private final RedisTemplate<String, String> stringRedisTemplateCustom;
    private final CodeExecutionOnDockerService judgeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisJudgeListener(
            @Qualifier("stringRedisTemplateCustom") RedisTemplate<String, String> stringRedisTemplateCustom,
            CodeExecutionOnDockerService judgeService) {
        this.stringRedisTemplateCustom = stringRedisTemplateCustom;
        this.judgeService = judgeService;
    }

    @PostConstruct
    public void startListener() {
        new Thread(() -> {
            log.info("Redis Judge Listener started. Monitoring 'judge_queue'...");
            while (true) {
            	try {
                    String message = stringRedisTemplateCustom.opsForList().rightPop("judge_queue", 10, TimeUnit.SECONDS);
                    
                    if (message != null) {
                        log.info("Received job: {}", message);
                        
                        // 메시지가 숫자(ID)인 경우 기존 호환성을 위해 SUBMIT으로 처리
                        if (message.matches("^\\d+$")) {
                            Long subId = Long.parseLong(message);
                            // 워커 스레드 내에서 레포지토리에 직접 접근 (서비스 호출 전 데이터 보강)
                            // 주의: Listener는 Service 계층이므로 Repository 주입 필요
                            judgeService.processLegacySubmit(subId);
                        } else {
                            // JSON인 경우 모드에 따라 처리
                            Map<String, Object> map = objectMapper.readValue(message, Map.class);
                            String id = map.get("id").toString();
                            String mode = (String) map.get("mode");
                            Long problemId = Long.valueOf(map.get("problemId").toString());
                            String code = (String) map.get("code");
                            String langStr = (String) map.getOrDefault("language", "JAVA");
                            com.passfail.enums.ProgrammingLanguage language = com.passfail.enums.ProgrammingLanguage.valueOf(langStr.toUpperCase());

                            if ("SUBMIT".equals(mode)) {
                                judgeService.judgeAsync(id, problemId, code, language);
                            } else if ("RUN".equals(mode)) {
                                List<CustomTestCaseRequest> customCases = null;
                                if (map.containsKey("customTestCases")) {
                                    customCases = objectMapper.convertValue(map.get("customTestCases"), 
                                            objectMapper.getTypeFactory().constructCollectionType(List.class, CustomTestCaseRequest.class));
                                }
                                judgeService.runAsync(id, problemId, code, customCases, language);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Error in Redis Judge Listener", e);
                    try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                }
            }
        }, "redis-judge-worker").start();
    }
}
