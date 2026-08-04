package com.chuckchuck.kiosk;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class KioskSessionStore {
    static final Duration SESSION_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "kiosk:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public KioskSessionStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<KioskSessionState> find(String sessionId) {
        String value = redisTemplate.opsForValue().get(key(sessionId));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, KioskSessionState.class));
        } catch (JsonProcessingException exception) {
            clear(sessionId);
            throw new ApiException(ErrorCode.SESSION_EXPIRED);
        }
    }

    public void save(KioskSessionState state) {
        try {
            redisTemplate.opsForValue().set(
                    key(state.sessionId()),
                    objectMapper.writeValueAsString(state),
                    SESSION_TTL
            );
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public void clear(String sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
