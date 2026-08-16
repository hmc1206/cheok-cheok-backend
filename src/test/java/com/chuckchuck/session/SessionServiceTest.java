package com.chuckchuck.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.chuckchuck.voice.Intent;
import com.fasterxml.jackson.databind.ObjectMapper;

class SessionServiceTest {
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private SessionService sessionService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = new ObjectMapper();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        sessionService = new SessionService(redisTemplate, objectMapper);
    }

    @Test
    void savesSessionForTenMinutes() {
        SessionState state = new SessionState("u123", Intent.MAP_ROUTE, "ASK_ORIGIN", Map.of());

        sessionService.save(state);

        verify(valueOperations).set("voice:session:u123", objectMapperValue(state), Duration.ofMinutes(10));
    }

    @Test
    void readsSlotsContainingNullValues() throws Exception {
        Map<String, Object> slots = new LinkedHashMap<>();
        slots.put("origin", null);
        slots.put("destination", "부산");
        SessionState state = new SessionState("u123", Intent.MAP_ROUTE, "ASK_ORIGIN", slots);
        when(valueOperations.get("voice:session:u123")).thenReturn(objectMapper.writeValueAsString(state));

        SessionState loaded = sessionService.find("u123").orElseThrow();

        assertThat(loaded.slots()).containsEntry("origin", null).containsEntry("destination", "부산");
    }

    @Test
    void clearsSession() {
        sessionService.clear("u123");

        verify(redisTemplate).delete("voice:session:u123");
    }

    private String objectMapperValue(SessionState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
