package com.chuckchuck.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.VoiceResponse;

class WeatherIntentHandlerTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 16);

    private WeatherService weatherService;
    private WeatherIntentHandler handler;

    @BeforeEach
    void setUp() {
        weatherService = mock(WeatherService.class);
        when(weatherService.today()).thenReturn(TODAY);
        when(weatherService.parseDate("2026-08-17")).thenReturn(TODAY.plusDays(1));
        handler = new WeatherIntentHandler(weatherService);
    }

    @Test
    void asksForLocationAndKeepsRequestedDate() {
        VoiceResponse response = handler.handle(newSession(), "내일 날씨 알려줘");

        assertThat(response.step()).isEqualTo("ASK_LOCATION");
        assertThat(response.screen()).isEqualTo("WEATHER_INPUT");
        assertThat(response.slots()).containsEntry("forecastDate", "2026-08-17");
    }

    @Test
    void looksUpNamedLocationFromFollowUp() {
        WeatherData data = weatherData();
        when(weatherService.lookup("서울", null, null, TODAY.plusDays(1))).thenReturn(data);

        VoiceResponse response = handler.handle(
                new SessionState(
                        "u123",
                        Intent.WEATHER_INFO,
                        "ASK_LOCATION",
                        Map.of("forecastDate", "2026-08-17")
                ),
                "서울"
        );

        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.screen()).isEqualTo("WEATHER_RESULT");
        assertThat(response.ttsText()).contains("내일", "서울특별시", "비예요", "우산");
        verify(weatherService).lookup("서울", null, null, TODAY.plusDays(1));
    }

    private SessionState newSession() {
        return new SessionState("u123", Intent.WEATHER_INFO, "NEW", Map.of());
    }

    private WeatherData weatherData() {
        return new WeatherData(
                new WeatherData.Location("서울특별시", 37.5665, 126.978),
                TODAY.plusDays(1),
                OffsetDateTime.parse("2026-08-16T15:00:00+09:00"),
                "RAIN",
                "비",
                null,
                null,
                24.0,
                29.0,
                70,
                null,
                2.4,
                true,
                "외출하실 때 우산을 챙기세요."
        );
    }
}
