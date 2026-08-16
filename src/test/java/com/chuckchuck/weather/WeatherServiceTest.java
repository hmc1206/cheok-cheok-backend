package com.chuckchuck.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class WeatherServiceTest {

    @Test
    void mapsRainForecastAndRecommendsUmbrella() {
        WeatherApiClient apiClient = mock(WeatherApiClient.class);
        WeatherService service = new WeatherService(apiClient);
        LocalDate today = service.today();
        WeatherApiClient.ResolvedLocation location = new WeatherApiClient.ResolvedLocation(
                "서울특별시", 37.5665, 126.978
        );
        WeatherApiClient.Current current = new WeatherApiClient.Current(27.1, 29.0, 78, 61, 2.1);
        WeatherApiClient.Forecast forecast = new WeatherApiClient.Forecast(61, 24.0, 29.0, 70, 4.2, current);
        when(apiClient.resolve("서울")).thenReturn(location);
        when(apiClient.forecast(location.latitude(), location.longitude(), today)).thenReturn(forecast);

        WeatherData result = service.lookup("서울", null, null, today);

        assertThat(result.conditionCode()).isEqualTo("RAIN");
        assertThat(result.currentTemperature()).isEqualTo(27.1);
        assertThat(result.umbrellaRecommended()).isTrue();
        assertThat(result.advice()).contains("우산");
    }
}
