package com.chuckchuck.weather;

import java.time.LocalDate;
import java.time.OffsetDateTime;

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
