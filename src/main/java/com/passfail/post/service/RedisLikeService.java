package com.passfail.post.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

// ✅ 수정: @RequiredArgsConstructor 제거
// → @Qualifier가 필요한 수동 생성자와 Lombok 자동 생성자가 공존하면 컴파일 오류 발생
@Service
public class RedisLikeService {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisLikeService(
            @Qualifier("stringRedisTemplateCustom") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String LIKE_KEY_PREFIX = "post:like:";

    public boolean addLike(Long postId, Long memberId) {
        String key = LIKE_KEY_PREFIX + postId;
        Long result = redisTemplate.opsForSet().add(key, memberId.toString());
        return result != null && result > 0;
    }

    public boolean removeLike(Long postId, Long memberId) {
        String key = LIKE_KEY_PREFIX + postId;
        Long result = redisTemplate.opsForSet().remove(key, memberId.toString());
        return result != null && result > 0;
    }

    public boolean isLiked(Long postId, Long memberId) {
        String key = LIKE_KEY_PREFIX + postId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, memberId.toString()));
    }

    public long getLikeCount(Long postId) {
        String key = LIKE_KEY_PREFIX + postId;
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0;
    }
}