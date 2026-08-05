package com.chuckchuck.kiosk;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;

class KioskSessionStoreTest {
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private KioskSessionStore store;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        store = new KioskSessionStore(redisTemplate, new ObjectMapper());
    }

    @Test
    void savesSessionForTenMinutes() {
        store.save(new KioskSessionState("ks_001", "kiosk_cafe", 0));

        verify(valueOperations).set(
                eq("kiosk:session:ks_001"),
                anyString(),
                eq(Duration.ofMinutes(10))
        );
    }
}
