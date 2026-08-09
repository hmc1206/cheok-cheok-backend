package com.chuckchuck.voice;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.chuckchuck.session.SessionState;

@Component
public class UnknownIntentHandler implements IntentHandler {

    @Override
    public Intent supports() {
        return Intent.UNKNOWN;
    }

    @Override
    public VoiceResponse handle(SessionState session, String userText) {
        return new VoiceResponse(
                Intent.UNKNOWN,
                "DONE",
                Map.of(),
                "죄송해요. 유튜브, 길찾기, 기차역 안내, 키오스크 연습 중 하나를 말씀해 주세요.",
                "VOICE_INPUT",
                null
        );
    }
}
