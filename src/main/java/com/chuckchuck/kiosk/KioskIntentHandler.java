package com.chuckchuck.kiosk;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.IntentHandler;
import com.chuckchuck.voice.VoiceResponse;

@Component
public class KioskIntentHandler implements IntentHandler {
    private final KioskService kioskService;

    public KioskIntentHandler(KioskService kioskService) {
        this.kioskService = kioskService;
    }

    @Override
    public Intent supports() {
        return Intent.KIOSK_TRAIN;
    }

    @Override
    public VoiceResponse handle(SessionState session, String userText) {
        return new VoiceResponse(
                Intent.KIOSK_TRAIN,
                "DONE",
                Map.of(),
                "키오스크 연습을 시작할게요. 연습할 화면을 골라 주세요.",
                "KIOSK_SCENARIOS",
                kioskService.findScenarios()
        );
    }
}
