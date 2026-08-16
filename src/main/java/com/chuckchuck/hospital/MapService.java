package com.chuckchuck.hospital;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MapService {
    private static final String NAVER_LOCAL_SEARCH_URL =
            "https://naverapihub.apigw.ntruss.com";
    private static final double NAVER_COORDINATE_SCALE = 10_000_000d;

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKeyId;
    private final String apiKey;
    private final String appName;

    public MapService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${naver.search.client-id:}") String apiKeyId,
            @Value("${naver.search.client-secret:}") String apiKey,
            @Value("${app.naver-map.app-name:com.chuckchuck.app}") String appName
    ) {
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(NAVER_LOCAL_SEARCH_URL).build();
        this.apiKeyId = apiKeyId.trim();
        this.apiKey = apiKey.trim();
        this.appName = appName.trim();
    }

    public MedicalRouteResponseDto processMedicalRoute(MedicalRouteRequestDto request) {
        validate(request);
        String facilityType = "PHARMACY".equalsIgnoreCase(request.type()) ? "약국" : "병원";
        JsonNode place = searchNaverLocal(facilityType, request.latitude(), request.longitude());
        String title = cleanTitle(place.path("title").asText());
        double goalLongitude = coordinate(place.path("mapx").asText(), 180);
        double goalLatitude = coordinate(place.path("mapy").asText(), 90);

        String appUrl = createNaverMapAppUrl(
                request.latitude(), request.longitude(), goalLatitude, goalLongitude, title
        );
        String webUrl = createNaverMapWebUrl(
                request.latitude(), request.longitude(), goalLatitude, goalLongitude, title
        );
        String ttsText = "주변 " + facilityType + " 검색 결과를 찾았어요. 목적지는 "
                + title + "입니다. 네이버 지도를 열게요.";

        return new MedicalRouteResponseDto(
                "MEDICAL_ROUTE",
                "DONE",
                ttsText,
                "NAVER_MAP_VIEW",
                new MedicalRouteResponseDto.RouteData(appUrl, webUrl)
        );
    }

    private void validate(MedicalRouteRequestDto request) {
        boolean validType = request != null
                && ("HOSPITAL".equalsIgnoreCase(request.type())
                || "PHARMACY".equalsIgnoreCase(request.type()));
        boolean validCoordinates = request != null
                && request.latitude() != null
                && request.longitude() != null
                && request.latitude() >= -90 && request.latitude() <= 90
                && request.longitude() >= -180 && request.longitude() <= 180;
        if (!validType || !validCoordinates) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "의료시설 종류와 현재 위치를 올바르게 입력해야 합니다.",
                    "병원이나 약국과 현재 위치를 다시 확인해 주세요."
            );
        }
        if (apiKeyId.isBlank() || apiKey.isBlank()) {
            throw new ApiException(
                    ErrorCode.GEOCODE_API_FAIL,
                    "NAVER_SEARCH_CLIENT_ID와 NAVER_SEARCH_CLIENT_SECRET이 설정되지 않았습니다.",
                    "지도 검색 설정을 확인해 주세요."
            );
        }
    }

    private JsonNode searchNaverLocal(String query, double latitude, double longitude) {
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/v1/local")
                            .queryParam("query", query)
                            .queryParam("display", 1)
                            .queryParam("sort", "random")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .build())
                    .header("X-NCP-APIGW-API-KEY-ID", apiKeyId)
                    .header("X-NCP-APIGW-API-KEY", apiKey)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .body(String.class);
            JsonNode items = objectMapper.readTree(body).path("items");
            if (!items.isArray() || items.isEmpty()) {
                throw new ApiException(
                        ErrorCode.GEOCODE_NOT_FOUND,
                        "검색된 의료시설이 없습니다.",
                        "주변 의료시설을 찾지 못했어요. 위치를 바꿔 다시 시도해 주세요."
                );
            }
            return items.get(0);
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientException | JsonProcessingException | IllegalArgumentException exception) {
            throw new ApiException(
                    ErrorCode.GEOCODE_API_FAIL,
                    "네이버 의료시설 검색 결과를 처리할 수 없습니다.",
                    "지금은 병원이나 약국을 찾지 못했어요. 잠시 후 다시 해 주세요."
            );
        }
    }

    private double coordinate(String value, double limit) {
        try {
            double coordinate = Double.parseDouble(value);
            if (Math.abs(coordinate) > limit) {
                coordinate /= NAVER_COORDINATE_SCALE;
            }
            if (Math.abs(coordinate) > limit) {
                throw new NumberFormatException("out of range");
            }
            return coordinate;
        } catch (NumberFormatException exception) {
            throw new ApiException(
                    ErrorCode.GEOCODE_API_FAIL,
                    "검색된 의료시설의 좌표를 읽을 수 없습니다.",
                    "지도 위치를 확인하지 못했어요. 잠시 후 다시 해 주세요."
            );
        }
    }

    private String createNaverMapAppUrl(
            double startLatitude,
            double startLongitude,
            double goalLatitude,
            double goalLongitude,
            String goalName
    ) {
        return "nmap://route/public"
                + "?slat=" + startLatitude
                + "&slng=" + startLongitude
                + "&sname=" + encode("현재 위치")
                + "&dlat=" + goalLatitude
                + "&dlng=" + goalLongitude
                + "&dname=" + encode(goalName)
                + "&appname=" + encode(appName);
    }

    private String createNaverMapWebUrl(
            double startLatitude,
            double startLongitude,
            double goalLatitude,
            double goalLongitude,
            String goalName
    ) {
        return String.format(
                "https://map.naver.com/p/directions/%s,%s,%s/%s,%s,%s/-/public",
                startLongitude,
                startLatitude,
                encode("현재 위치"),
                goalLongitude,
                goalLatitude,
                encode(goalName)
        );
    }

    private String cleanTitle(String title) {
        if (title == null || title.isBlank()) {
            return "의료시설";
        }
        return title.replaceAll("<[^>]*>", "").trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
