package com.chuckchuck.voice;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * text는 큰 선택 버튼 값, audio는 사용자가 녹음한 Base64 원본이다.
 * 이전 프론트의 audioBase64 이름도 JsonAlias로 받아 전환 기간의 호환성을 유지한다.
 * latitude와 longitude는 현재 위치 날씨 조회에만 선택적으로 사용하며 반드시 함께 보낸다.
 */
public record VoiceRequest(
        String userId,
        String text,
        @JsonAlias("audioBase64") String audio,
        Double latitude,
        Double longitude
) {
    public VoiceRequest(String userId, String text, String audio) {
        this(userId, text, audio, null, null);
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }

    public boolean hasAudio() {
        return audio != null && !audio.isBlank();
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}
