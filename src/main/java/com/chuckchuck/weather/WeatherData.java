package com.chuckchuck.weather;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 외부 날씨 제공업체의 형식과 분리한 프론트 공통 응답 계약이다. */
public record WeatherData(
        Location location,
        LocalDate forecastDate,
        OffsetDateTime updatedAt,
        String conditionCode,
        String conditionText,
        Double currentTemperature,
        Double feelsLikeTemperature,
        Double minimumTemperature,
        Double maximumTemperature,
        Integer precipitationProbability,
        Integer humidity,
        Double windSpeed,
        boolean umbrellaRecommended,
        String advice
) {
    public record Location(String name, double latitude, double longitude) {
    }
}
