package com.chuckchuck.hospital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.VoiceResponse;

class MedicalIntentHandlerTest {
    private MapService mapService;
    private MedicalIntentHandler handler;

    @BeforeEach
    void setUp() {
        mapService = mock(MapService.class);
        handler = new MedicalIntentHandler(mapService);
    }

    @Test
    void asksForLocationAndKeepsPharmacyType() {
        VoiceResponse response = handler.handle(newSession(Map.of()), "가까운 약국 찾아줘");

        assertThat(response.step()).isEqualTo("ASK_LOCATION");
        assertThat(response.slots()).containsEntry("type", "PHARMACY");
        assertThat(response.ttsText()).contains("약국", "위치 권한");
        assertThat(response.screen()).isEqualTo("MEDICAL_INPUT");
    }

    @Test
    void returnsPharmacyRouteWithTts() {
        MedicalRouteResponseDto serviceResponse = new MedicalRouteResponseDto(
                "MEDICAL_ROUTE",
                "DONE",
                "주변 약국 검색 결과를 찾았어요. 목적지는 새봄약국입니다. 네이버 지도를 열게요.",
                "NAVER_MAP_VIEW",
                new MedicalRouteResponseDto.RouteData("nmap://route/public", "https://map.naver.com")
        );
        when(mapService.processMedicalRoute(argThat(request ->
                request.type().equals("PHARMACY")
                        && request.latitude() == 37.5665
                        && request.longitude() == 126.978
        ))).thenReturn(serviceResponse);

        VoiceResponse response = handler.handle(
                newSession(Map.of("latitude", 37.5665, "longitude", 126.978)),
                "가까운 약국 찾아줘"
        );

        assertThat(response.intent()).isEqualTo(Intent.MEDICAL_ROUTE);
        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.screen()).isEqualTo("NAVER_MAP_VIEW");
        assertThat(response.ttsText()).contains("새봄약국", "네이버 지도");
        verify(mapService).processMedicalRoute(argThat(request -> request.type().equals("PHARMACY")));
    }

    private SessionState newSession(Map<String, Object> slots) {
        return new SessionState("u123", Intent.MEDICAL_ROUTE, "NEW", slots);
    }
}
