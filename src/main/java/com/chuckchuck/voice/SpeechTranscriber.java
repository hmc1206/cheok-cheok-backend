package com.chuckchuck.voice;

/** VoiceService가 외부 STT 공급자의 세부 구현을 몰라도 되게 하는 경계다. */
public interface SpeechTranscriber {
    String transcribe(String audioBase64);
}
