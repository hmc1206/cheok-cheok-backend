package com.chuckchuck.weather;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class WeatherApiClient {
    private static final Map<String, String> KOREAN_REGION_ALIASES = Map.ofEntries(
            Map.entry("서울", "서울특별시"),
            Map.entry("부산", "부산광역시"),
            Map.entry("대구", "대구광역시"),
            Map.entry("인천", "인천광역시"),
            Map.entry("광주", "광주광역시"),
            Map.entry("대전", "대전광역시"),
            Map.entry("울산", "울산광역시"),
            Map.entry("세종", "세종특별자치시"),
            Map.entry("경기", "경기도"),
            Map.entry("강원", "강원특별자치도"),
            Map.entry("충북", "충청북도"),
            Map.entry("충남", "충청남도"),
            Map.entry("전북", "전북특별자치도"),
            Map.entry("전남", "전라남도"),
            Map.entry("경북", "경상북도"),
            Map.entry("경남", "경상남도"),
            Map.entry("제주", "제주특별자치도")
    );

    private final RestClient geocodingClient;
    private final RestClient forecastClient;
    private final String geocodingBaseUrl;

    @Autowired
    public WeatherApiClient(
            RestClient.Builder builder,
            @Value("${WEATHER_GEOCODING_BASE_URL:https://geocoding-api.open-meteo.com}") String geocodingBaseUrl,
            @Value("${WEATHER_FORECAST_BASE_URL:https://api.open-meteo.com}") String forecastBaseUrl
    ) {
        this.geocodingClient = builder.clone().build();
        this.forecastClient = builder.clone().baseUrl(forecastBaseUrl).build();
        this.geocodingBaseUrl = geocodingBaseUrl;
    }

    WeatherApiClient(RestClient geocodingClient, RestClient forecastClient) {
        this.geocodingClient = geocodingClient;
        this.forecastClient = forecastClient;
        this.geocodingBaseUrl = "https://geocoding-api.open-meteo.com";
    }

    public ResolvedLocation resolve(String locationName) {
        try {
            String normalizedName = KOREAN_REGION_ALIASES.getOrDefault(
                    locationName.replaceAll("\\s+", ""),
                    locationName
            );
            String encodedName = URLEncoder.encode(normalizedName, StandardCharsets.UTF_8).replace("+", "%20");
            URI uri = URI.create(geocodingBaseUrl
                    + "/v1/search?name=" + encodedName
                    + "&count=5&language=ko&countryCode=KR");
            GeocodingResponse response = geocodingClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(GeocodingResponse.class);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                throw new ApiException(ErrorCode.WEATHER_LOCATION_NOT_FOUND);
            }
            GeocodingResult first = response.results().getFirst();
            if (first == null || first.name() == null || first.name().isBlank()) {
                throw new ApiException(ErrorCode.WEATHER_LOCATION_NOT_FOUND);
            }
            String name = first.admin1() == null
                    ? first.name()
                    : first.admin1().contains(first.name())
                            ? first.admin1()
                            : first.admin1() + " " + first.name();
            return new ResolvedLocation(name, first.latitude(), first.longitude());
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ApiException(ErrorCode.WEATHER_API_FAIL);
        }
    }

    public Forecast forecast(double latitude, double longitude, LocalDate date) {
        try {
            ForecastResponse response = forecastClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("timezone", "Asia/Seoul")
                            .queryParam("start_date", date)
                            .queryParam("end_date", date)
                            .queryParam("current", "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m")
                            .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max")
                            .queryParam("wind_speed_unit", "ms")
                            .build())
                    .retrieve()
                    .body(ForecastResponse.class);

            if (response == null || response.daily() == null || response.daily().time() == null
                    || response.daily().time().isEmpty()) {
                throw new ApiException(ErrorCode.WEATHER_DATA_NOT_FOUND);
            }
            Daily daily = response.daily();
            return new Forecast(
                    number(daily.weatherCode(), 0),
                    decimal(daily.minimumTemperature(), 0),
                    decimal(daily.maximumTemperature(), 0),
                    number(daily.precipitationProbability(), 0),
                    decimal(daily.windSpeed(), 0),
                    response.current()
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ApiException(ErrorCode.WEATHER_API_FAIL);
        }
    }

    private Integer number(List<Integer> values, int index) {
        return values == null || values.size() <= index ? null : values.get(index);
    }

    private Double decimal(List<Double> values, int index) {
        return values == null || values.size() <= index ? null : values.get(index);
    }

    public record ResolvedLocation(String name, double latitude, double longitude) {
    }

    public record Forecast(
            Integer weatherCode,
            Double minimumTemperature,
            Double maximumTemperature,
            Integer precipitationProbability,
            Double maximumWindSpeed,
            Current current
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeocodingResponse(List<GeocodingResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeocodingResult(String name, double latitude, double longitude, String admin1) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ForecastResponse(Current current, Daily daily) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(
            @JsonProperty("temperature_2m") Double temperature,
            @JsonProperty("apparent_temperature") Double apparentTemperature,
            @JsonProperty("relative_humidity_2m") Integer humidity,
            @JsonProperty("weather_code") Integer weatherCode,
            @JsonProperty("wind_speed_10m") Double windSpeed
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Daily(
            List<String> time,
            @JsonProperty("weather_code") List<Integer> weatherCode,
            @JsonProperty("temperature_2m_min") List<Double> minimumTemperature,
            @JsonProperty("temperature_2m_max") List<Double> maximumTemperature,
            @JsonProperty("precipitation_probability_max") List<Integer> precipitationProbability,
            @JsonProperty("wind_speed_10m_max") List<Double> windSpeed
    ) {
    }
}
