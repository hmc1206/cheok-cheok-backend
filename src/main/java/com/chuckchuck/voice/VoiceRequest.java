package com.chuckchuck.voice;

public record VoiceRequest(String userId, String text, String audioBase64) {
    public boolean hasText() {
        return text != null && !text.isBlank();
    }

    public boolean hasAudio() {
        return audioBase64 != null && !audioBase64.isBlank();
    }
}
