package com.passfail.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class VisitorService {

    private final RedisTemplate<String, String> stringRedisTemplateCustom;
    private static final String VISITOR_KEY_PREFIX = "visitor:";

    public VisitorService(@Qualifier("stringRedisTemplateCustom") RedisTemplate<String, String> stringRedisTemplateCustom) {
        this.stringRedisTemplateCustom = stringRedisTemplateCustom;
    }

    public void incrementVisitor(String ipAddress) {
        String key = VISITOR_KEY_PREFIX + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        stringRedisTemplateCustom.opsForSet().add(key, ipAddress);
    }

    public long getTodayVisitorCount() {
        try {
            String key = VISITOR_KEY_PREFIX + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            Long size = stringRedisTemplateCustom.opsForSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.warn("Failed to get visitor count from Redis: {}", e.getMessage());
            return 0;
        }
    }
}
