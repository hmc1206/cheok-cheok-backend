package com.chuckchuck.kiosk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.VoiceResponse;

class KioskIntentHandlerTest {

    @Test
    void routesVoiceRequestToScenarioScreen() {
        KioskService service = mock(KioskService.class);
        when(service.findScenarios()).thenReturn(new KioskScenarioListResponse(List.of()));
        KioskIntentHandler handler = new KioskIntentHandler(service);

        VoiceResponse response = handler.handle(
                new SessionState("u123", Intent.KIOSK_HELP, "NEW", Map.of()),
                "키오스크 연습할래"
        );

        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.screen()).isEqualTo("KIOSK_SCENARIOS");
    }
}
