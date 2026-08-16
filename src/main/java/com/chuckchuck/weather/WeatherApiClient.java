package com.chuckchuck.weather;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 지역명은 좌표로 바꾸고, 실제 예보 값은 기상청 동네예보 격자자료에서 조회한다.
 * 기상청의 전체 격자 응답은 같은 발표시각 동안 캐시해 사용자마다 다시 내려받지 않는다.
 */
@Component
public class WeatherApiClient {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final int GRID_WIDTH = 149;
    private static final int GRID_HEIGHT = 253;
    private static final int GRID_VALUE_COUNT = GRID_WIDTH * GRID_HEIGHT;
    private static final int MAX_CACHE_ENTRIES = 64;
    private static final int[] FORECAST_ISSUE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};

    // 사용자는 "서울"처럼 짧게 말하므로 국내 행정구역의 정식 명칭으로 보정한다.
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
    private final HttpClient forecastClient;
    private final String geocodingBaseUrl;
    private final String forecastBaseUrl;
    private final String apiKey;
    private final ConcurrentMap<GridRequest, CompletableFuture<double[]>> gridCache = new ConcurrentHashMap<>();

    public WeatherApiClient(
            RestClient.Builder builder,
            @Value("${WEATHER_GEOCODING_BASE_URL:https://geocoding-api.open-meteo.com}") String geocodingBaseUrl,
            @Value("${KMA_API_BASE_URL:https://apihub.kma.go.kr}") String forecastBaseUrl,
            @Value("${KMA_API_AUTH_KEY:}") String apiKey
    ) {
        this.geocodingClient = builder.clone().build();
        this.forecastClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.geocodingBaseUrl = geocodingBaseUrl;
        this.forecastBaseUrl = forecastBaseUrl;
        this.apiKey = apiKey.trim();
    }

    public ResolvedLocation resolve(String locationName) {
        try {
            String normalizedName = KOREAN_REGION_ALIASES.getOrDefault(
                    locationName.replaceAll("\\s+", ""),
                    locationName
            );
            // 한글 검색어가 다시 인코딩되지 않도록 UTF-8로 한 번 인코딩한 절대 URI를 사용한다.
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
        if (apiKey.isBlank()) {
            throw new ApiException(ErrorCode.WEATHER_API_FAIL);
        }

        GridPoint point;
        try {
            point = toGridPoint(latitude, longitude);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.WEATHER_LOCATION_NOT_FOUND);
        }

        ZonedDateTime now = ZonedDateTime.now(KOREA);
        LocalDateTime baseTime = latestBaseTime(now);
        LocalDateTime targetTime = date.equals(now.toLocalDate())
                ? date.atTime(now.getHour(), 0)
                : date.atTime(12, 0);
        LocalDateTime dailyBaseTime = dailyBaseTime(now, baseTime, date);

        try {
            CompletableFuture<Double> temperature = gridValue(baseTime, targetTime, "TMP", point);
            CompletableFuture<Double> sky = gridValue(baseTime, targetTime, "SKY", point);
            CompletableFuture<Double> precipitationType = gridValue(baseTime, targetTime, "PTY", point);
            CompletableFuture<Double> precipitationProbability = gridValue(baseTime, targetTime, "POP", point);
            CompletableFuture<Double> humidity = gridValue(baseTime, targetTime, "REH", point);
            CompletableFuture<Double> windSpeed = gridValue(baseTime, targetTime, "WSD", point);
            CompletableFuture<Double> minimumTemperature = gridValue(
                    dailyBaseTime,
                    date.atTime(6, 0),
                    "TMN",
                    point
            );
            CompletableFuture<Double> maximumTemperature = gridValue(
                    dailyBaseTime,
                    date.atTime(15, 0),
                    "TMX",
                    point
            );

            CompletableFuture.allOf(
                    temperature,
                    sky,
                    precipitationType,
                    precipitationProbability,
                    humidity,
                    windSpeed,
                    minimumTemperature,
                    maximumTemperature
            ).join();

            Double temperatureValue = temperature.join();
            Integer skyCode = integer(sky.join());
            Integer precipitationTypeCode = integer(precipitationType.join());
            if (temperatureValue == null && skyCode == null && precipitationTypeCode == null) {
                throw new ApiException(ErrorCode.WEATHER_DATA_NOT_FOUND);
            }

            Current current = date.equals(now.toLocalDate())
                    ? new Current(
                            temperatureValue,
                            null,
                            integer(humidity.join()),
                            skyCode,
                            precipitationTypeCode,
                            windSpeed.join()
                    )
                    : null;

            return new Forecast(
                    skyCode,
                    precipitationTypeCode,
                    minimumTemperature.join(),
                    maximumTemperature.join(),
                    integer(precipitationProbability.join()),
                    windSpeed.join(),
                    current
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (CompletionException | IllegalStateException exception) {
            throw new ApiException(ErrorCode.WEATHER_API_FAIL);
        }
    }

    private CompletableFuture<Double> gridValue(
            LocalDateTime baseTime,
            LocalDateTime targetTime,
            String variable,
            GridPoint point
    ) {
        GridRequest request = new GridRequest(baseTime, targetTime, variable);
        if (gridCache.size() >= MAX_CACHE_ENTRIES) {
            gridCache.clear();
        }
        CompletableFuture<double[]> grid = gridCache.computeIfAbsent(request, this::requestGrid);
        return grid.handle((values, error) -> {
            if (error != null) {
                gridCache.remove(request, grid);
                throw new CompletionException(error);
            }
            double value = values[point.index()];
            return Double.isNaN(value) ? null : value;
        });
    }

    private CompletableFuture<double[]> requestGrid(GridRequest request) {
        return sendGridRequest(request)
                .handle((values, error) -> error == null
                        ? CompletableFuture.completedFuture(values)
                        : sendGridRequest(request))
                .thenCompose(result -> result);
    }

    // 기상청이 순간적으로 연결을 끊는 경우가 있어 실패한 격자 한 건만 즉시 한 번 재시도한다.
    private CompletableFuture<double[]> sendGridRequest(GridRequest request) {
        String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8).replace("+", "%20");
        URI uri = URI.create(forecastBaseUrl
                + "/api/typ01/cgi-bin/url/nph-dfs_shrt_grd"
                + "?tmfc=" + request.baseTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHH"))
                + "&tmef=" + request.targetTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHH"))
                + "&vars=" + request.variable()
                + "&authKey=" + encodedKey);
        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(45))
                .GET()
                .build();

        return forecastClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("KMA weather API returned " + response.statusCode());
                    }
                    return parseGrid(response.body());
                });
    }

    static double[] parseGrid(String body) {
        String[] tokens = body == null ? new String[0] : body.trim().split("[,\\s]+");
        if (tokens.length != GRID_VALUE_COUNT) {
            throw new IllegalStateException("Unexpected KMA grid size");
        }

        double[] values = new double[GRID_VALUE_COUNT];
        for (int index = 0; index < tokens.length; index++) {
            double value = Double.parseDouble(tokens[index]);
            values[index] = value <= -90 ? Double.NaN : value;
        }
        return values;
    }

    // 기상청의 Lambert Conformal Conic 공식으로 위·경도를 5km 동네예보 격자로 바꾼다.
    static GridPoint toGridPoint(double latitude, double longitude) {
        double earthRadius = 6371.00877 / 5.0;
        double firstStandardLatitude = Math.toRadians(30.0);
        double secondStandardLatitude = Math.toRadians(60.0);
        double originLongitude = Math.toRadians(126.0);
        double originLatitude = Math.toRadians(38.0);
        double sn = Math.log(Math.cos(firstStandardLatitude) / Math.cos(secondStandardLatitude))
                / Math.log(
                        Math.tan(Math.PI * 0.25 + secondStandardLatitude * 0.5)
                                / Math.tan(Math.PI * 0.25 + firstStandardLatitude * 0.5)
                );
        double sf = Math.pow(Math.tan(Math.PI * 0.25 + firstStandardLatitude * 0.5), sn)
                * Math.cos(firstStandardLatitude) / sn;
        double ro = earthRadius * sf
                / Math.pow(Math.tan(Math.PI * 0.25 + originLatitude * 0.5), sn);
        double ra = earthRadius * sf
                / Math.pow(Math.tan(Math.PI * 0.25 + Math.toRadians(latitude) * 0.5), sn);
        double theta = Math.toRadians(longitude) - originLongitude;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int x = (int) Math.floor(ra * Math.sin(theta) + 43.0 + 0.5);
        int y = (int) Math.floor(ro - ra * Math.cos(theta) + 136.0 + 0.5);
        if (x < 1 || x > GRID_WIDTH || y < 1 || y > GRID_HEIGHT) {
            throw new IllegalArgumentException("Location is outside the KMA forecast grid");
        }
        return new GridPoint(x, y);
    }

    static LocalDateTime latestBaseTime(ZonedDateTime now) {
        ZonedDateTime readyTime = now.minusMinutes(20);
        for (int index = FORECAST_ISSUE_HOURS.length - 1; index >= 0; index--) {
            int hour = FORECAST_ISSUE_HOURS[index];
            if (readyTime.getHour() >= hour) {
                return readyTime.toLocalDate().atTime(hour, 0);
            }
        }
        return readyTime.toLocalDate().minusDays(1).atTime(23, 0);
    }

    private LocalDateTime dailyBaseTime(ZonedDateTime now, LocalDateTime latestBaseTime, LocalDate date) {
        LocalTime firstIssueReady = LocalTime.of(2, 20);
        if (date.equals(now.toLocalDate()) && !now.toLocalTime().isBefore(firstIssueReady)) {
            return date.atTime(2, 0);
        }
        return latestBaseTime;
    }

    private Integer integer(Double value) {
        return value == null ? null : (int) Math.round(value);
    }

    public record ResolvedLocation(String name, double latitude, double longitude) {
    }

    public record Forecast(
            Integer skyCode,
            Integer precipitationType,
            Double minimumTemperature,
            Double maximumTemperature,
            Integer precipitationProbability,
            Double windSpeed,
            Current current
    ) {
    }

    public record Current(
            Double temperature,
            Double apparentTemperature,
            Integer humidity,
            Integer skyCode,
            Integer precipitationType,
            Double windSpeed
    ) {
    }

    record GridPoint(int x, int y) {
        int index() {
            // 기상청 파일은 좌하단부터 x가 먼저 증가하는 순서로 저장된다.
            return (y - 1) * GRID_WIDTH + (x - 1);
        }
    }

    private record GridRequest(LocalDateTime baseTime, LocalDateTime targetTime, String variable) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeocodingResponse(List<GeocodingResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeocodingResult(String name, double latitude, double longitude, String admin1) {
    }
}
