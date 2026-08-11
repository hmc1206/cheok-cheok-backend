package com.chuckchuck.voice;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * text는 큰 선택 버튼 값, audio는 사용자가 녹음한 Base64 원본이다.
 * 이전 프론트의 audioBase64 이름도 JsonAlias로 받아 전환 기간의 호환성을 유지한다.
 */
public record VoiceRequest(String userId, String text, @JsonAlias("audioBase64") String audio) {
    public boolean hasText() {
        return text != null && !text.isBlank();
    }

    public boolean hasAudio() {
        return audio != null && !audio.isBlank();
    }
}
