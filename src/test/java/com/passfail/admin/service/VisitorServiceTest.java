package com.passfail.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class VisitorServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private VisitorService visitorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void incrementVisitor_shouldAddIpToSet() {
        String ip = "127.0.0.1";
        String key = "visitor:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        visitorService.incrementVisitor(ip);

        verify(setOperations, times(1)).add(key, ip);
    }

    @Test
    void getTodayVisitorCount_shouldReturnSizeFromRedis() {
        String key = "visitor:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        when(setOperations.size(key)).thenReturn(5L);

        long count = visitorService.getTodayVisitorCount();

        assertEquals(5L, count);
        verify(setOperations, times(1)).size(key);
    }

    @Test
    void getTodayVisitorCount_shouldReturnZeroOnFailure() {
        when(setOperations.size(anyString())).thenThrow(new RuntimeException("Redis error"));

        long count = visitorService.getTodayVisitorCount();

        assertEquals(0L, count);
    }
}
