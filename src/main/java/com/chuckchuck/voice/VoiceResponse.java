package com.chuckchuck.voice;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record VoiceResponse(
        Intent intent,
        String step,
        Map<String, Object> slots,
        String ttsText,
        String screen,
        List<QuickReply> quickReplies,
        Object data
) {
    public VoiceResponse(
            Intent intent,
            String step,
            Map<String, Object> slots,
            String ttsText,
            String screen,
            Object data
    ) {
        this(intent, step, slots, ttsText, screen, null, data);
    }

    public VoiceResponse {
        slots = slots == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(slots));
        quickReplies = quickReplies == null ? null : List.copyOf(quickReplies);
    }
}
