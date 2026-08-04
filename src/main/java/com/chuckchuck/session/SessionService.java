package com.chuckchuck.session;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SessionService {
    static final Duration SESSION_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "voice:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SessionService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<SessionState> find(String userId) {
        String value = redisTemplate.opsForValue().get(key(userId));
        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, SessionState.class));
        } catch (JsonProcessingException exception) {
            // 손상된 세션을 계속 읽으며 실패하지 않도록 제거하고 새 대화를 시작할 수 있게 한다.
            clear(userId);
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public void save(SessionState state) {
        try {
            String value = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(key(state.userId()), value, SESSION_TTL);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public void clear(String userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }
}
