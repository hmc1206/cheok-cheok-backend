package com.chuckchuck.weather;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Service;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

/**
 * 지역명 또는 기기 좌표를 하나의 조회 흐름으로 합치고,
 * 외부 예보를 프론트가 사용하는 {@link WeatherData} 형식으로 변환한다.
 */
@Service
public class WeatherService {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final ZoneOffset KOREA_OFFSET = ZoneOffset.ofHours(9);
    private static final int MAX_FORECAST_DAYS = 5;

    private final WeatherApiClient apiClient;

    public WeatherService(WeatherApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public WeatherData lookup(String location, Double latitude, Double longitude, LocalDate date) {
        LocalDate forecastDate = date == null ? today() : date;
        validateDate(forecastDate);

        // 사용자가 말한 지역을 우선하고, 지역이 없을 때만 기기가 보낸 현재 좌표를 사용한다.
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
                    "날짜를 다시 말씀해 주세요."
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
        // 미래 날짜에는 "현재 기온"이 의미 없으므로 현재 관측값은 오늘 응답에만 포함한다.
        WeatherApiClient.Current current = today ? forecast.current() : null;
        Condition condition = Condition.from(
                current == null ? forecast.skyCode() : current.skyCode(),
                current == null ? forecast.precipitationType() : current.precipitationType()
        );
        int precipitationProbability = forecast.precipitationProbability() == null
                ? 0
                : forecast.precipitationProbability();
        // 강수확률이 낮거나 누락돼도 날씨 코드가 비·눈이면 우산을 안내한다.
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
                current == null ? forecast.windSpeed() : current.windSpeed(),
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
                    "위치를 확인하지 못했어요. 잠시 후 다시 해 주세요."
            );
        }
    }

    // 기상청 SKY·PTY 코드를 프론트가 안정적으로 처리할 수 있는 공통 코드로 줄인다.
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

        static Condition from(Integer skyCode, Integer precipitationType) {
            if (precipitationType != null) {
                if (precipitationType == 4) return SHOWER;
                if (precipitationType == 3 || precipitationType == 7) return SNOW;
                if (precipitationType == 1 || precipitationType == 2
                        || precipitationType == 5 || precipitationType == 6) return RAIN;
            }
            if (skyCode == null) return UNKNOWN;
            if (skyCode == 1) return CLEAR;
            if (skyCode == 3) return PARTLY_CLOUDY;
            if (skyCode == 4) return CLOUDY;
            return UNKNOWN;
        }
    }
}
