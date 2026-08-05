package com.chuckchuck.session;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.VoiceResponse;

public record SessionState(
        String userId,
        Intent intent,
        String step,
        Map<String, Object> slots
) {
    public SessionState {
        slots = slots == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(slots));
    }

    public static SessionState from(String userId, VoiceResponse response) {
        return new SessionState(userId, response.intent(), response.step(), response.slots());
    }
}
