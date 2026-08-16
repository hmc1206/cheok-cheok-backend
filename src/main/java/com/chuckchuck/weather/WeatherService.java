package com.chuckchuck.weather;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Service;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

@Service
public class WeatherService {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final ZoneOffset KOREA_OFFSET = ZoneOffset.ofHours(9);
    private static final int MAX_FORECAST_DAYS = 15;

    private final WeatherApiClient apiClient;

    public WeatherService(WeatherApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public WeatherData lookup(String location, Double latitude, Double longitude, LocalDate date) {
        LocalDate forecastDate = date == null ? today() : date;
        validateDate(forecastDate);

        WeatherApiClient.ResolvedLocation resolvedLocation;
        if (location != null && !location.isBlank()) {
            resolvedLocation = apiClient.resolve(location.trim());
        } else if (latitude != null && longitude != null) {
            validateCoordinates(latitude, longitude);
            resolvedLocation = new WeatherApiClient.ResolvedLocation("현재 위치", latitude, longitude);
        } else {
            throw new ApiException(ErrorCode.WEATHER_LOCATION_NOT_FOUND);
        }

        WeatherApiClient.Forecast forecast = apiClient.forecast(
                resolvedLocation.latitude(),
                resolvedLocation.longitude(),
                forecastDate
        );
        return toData(resolvedLocation, forecastDate, forecast);
    }

    public LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return today();
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "date는 YYYY-MM-DD 형식이어야 합니다.",
                    "날짜를 다시 확인해 주세요."
            );
        }
    }

    public LocalDate today() {
        return LocalDate.now(KOREA);
    }

    private WeatherData toData(
            WeatherApiClient.ResolvedLocation location,
            LocalDate date,
            WeatherApiClient.Forecast forecast
    ) {
        boolean today = date.equals(today());
        WeatherApiClient.Current current = today ? forecast.current() : null;
        Condition condition = Condition.from(
                current != null && current.weatherCode() != null
                        ? current.weatherCode()
                        : forecast.weatherCode()
        );
        int precipitationProbability = forecast.precipitationProbability() == null
                ? 0
                : forecast.precipitationProbability();
        boolean umbrellaRecommended = precipitationProbability >= 40 || condition.precipitation;
        String advice = umbrellaRecommended
                ? "외출하실 때 우산을 챙기세요."
                : "기온에 맞는 옷을 챙겨 입으세요.";

        return new WeatherData(
                new WeatherData.Location(location.name(), location.latitude(), location.longitude()),
                date,
                OffsetDateTime.now(KOREA_OFFSET),
                condition.code,
                condition.text,
                current == null ? null : current.temperature(),
                current == null ? null : current.apparentTemperature(),
                forecast.minimumTemperature(),
                forecast.maximumTemperature(),
                forecast.precipitationProbability(),
                current == null ? null : current.humidity(),
                current == null ? forecast.maximumWindSpeed() : current.windSpeed(),
                umbrellaRecommended,
                advice
        );
    }

    private void validateDate(LocalDate date) {
        LocalDate today = today();
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_FORECAST_DAYS))) {
            throw new ApiException(ErrorCode.WEATHER_DATE_NOT_SUPPORTED);
        }
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "위도와 경도의 범위를 확인해 주세요.",
                    "현재 위치 정보를 다시 확인해 주세요."
            );
        }
    }

    private enum Condition {
        CLEAR("CLEAR", "맑음", false),
        PARTLY_CLOUDY("PARTLY_CLOUDY", "구름 조금", false),
        CLOUDY("CLOUDY", "흐림", false),
        FOG("FOG", "안개", false),
        RAIN("RAIN", "비", true),
        SHOWER("SHOWER", "소나기", true),
        SNOW("SNOW", "눈", true),
        UNKNOWN("UNKNOWN", "날씨 정보 확인 불가", false);

        private final String code;
        private final String text;
        private final boolean precipitation;

        Condition(String code, String text, boolean precipitation) {
            this.code = code;
            this.text = text;
            this.precipitation = precipitation;
        }

        static Condition from(Integer code) {
            if (code == null) return UNKNOWN;
            if (code == 0) return CLEAR;
            if (code == 1 || code == 2) return PARTLY_CLOUDY;
            if (code == 3) return CLOUDY;
            if (code == 45 || code == 48) return FOG;
            if (code >= 80 && code <= 82) return SHOWER;
            if ((code >= 51 && code <= 67) || (code >= 95 && code <= 99)) return RAIN;
            if ((code >= 71 && code <= 77) || code == 85 || code == 86) return SNOW;
            return UNKNOWN;
        }
    }
}
