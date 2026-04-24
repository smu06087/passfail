package com.passfail.post.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

// ✅ 수정: @RequiredArgsConstructor 제거 (동일한 이유)
@Service
public class RedisViewService {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisViewService(
            @Qualifier("stringRedisTemplateCustom") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String VIEW_KEY_PREFIX = "post:view:";
    private static final Duration VIEW_TTL = Duration.ofHours(24);

    /**
     * @return true  → 새로운 조회 (카운트 증가 필요)
     *         false → 24시간 내 중복 조회 (카운트 증가 불필요)
     */
    public boolean isNewView(Long postId, Long memberId) {
        String key = VIEW_KEY_PREFIX + postId + ":" + memberId;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1", VIEW_TTL);
        return Boolean.TRUE.equals(isNew);
    }
}