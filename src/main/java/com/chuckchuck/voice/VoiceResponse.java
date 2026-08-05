package com.chuckchuck.voice;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record VoiceResponse(
        Intent intent,
        String step,
        Map<String, Object> slots,
        String ttsText,
        String screen,
        Object data
) {
    public VoiceResponse {
        slots = slots == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(slots));
    }
}
