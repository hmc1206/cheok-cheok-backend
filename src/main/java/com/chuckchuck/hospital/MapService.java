package com.chuckchuck.hospital;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Service
public class MapService {
    private static final String NAVER_LOCAL_SEARCH_URL =
            "https://naverapihub.apigw.ntruss.com";

    // 네이버 지역 검색 API는 latitude/longitude 파라미터를 아예 무시한다(좌표를 붙여도
    // 안 붙여도 응답이 완전히 동일하다). 그래서 "병원"만 검색하면 늘 전국 상위 결과인
    // 서울 광화문 병원이 나왔다 - 이번 버그의 원인이다. 지역 검색은 질의문(텍스트)만
    // 이해하므로, 좌표를 먼저 행정구역 이름으로 바꿔서 질의문에 넣어야 한다.
    //
    // ponytail: 좌표 -> 행정구역 변환은 키가 필요 없는 OSM Nominatim을 쓴다. 같은 일을
    // 하는 NCP Maps Reverse Geocoding은 이 프로젝트의 네이버 키로는 "이용 신청이
    // 필요합니다"(errorCode 210)로 막혀 있어 지금 당장 쓸 수 없다. Nominatim은 초당 1회
    // 제한이 있으니 트래픽이 늘면 NCP 콘솔에서 Reverse Geocoding 이용 신청을 한 뒤
    // reverseGeocode() 한 곳만 갈아끼우면 된다.
    private static final String REVERSE_GEOCODE_URL = "https://nominatim.openstreetmap.org";

    private static final double NAVER_COORDINATE_SCALE = 10_000_000d;
    private static final double EARTH_RADIUS_KM = 6_371d;

    private final RestClient localSearchClient;
    private final RestClient reverseGeocodeClient;
    private final String apiKeyId;
    private final String apiKey;
    private final String appName;

    public MapService(
            RestClient.Builder restClientBuilder,
            @Value("${naver.search.client-id:}") String apiKeyId,
            @Value("${naver.search.client-secret:}") String apiKey,
            @Value("${app.naver-map.app-name:com.chuckchuck.app}") String appName
    ) {
        this.localSearchClient = restClientBuilder.clone().baseUrl(NAVER_LOCAL_SEARCH_URL).build();
        this.reverseGeocodeClient = restClientBuilder.clone().baseUrl(REVERSE_GEOCODE_URL).build();
        this.apiKeyId = apiKeyId.trim();
        this.apiKey = apiKey.trim();
        this.appName = appName.trim();
    }

    public MedicalRouteResponseDto processMedicalRoute(MedicalRouteRequestDto request) {
        validate(request);
        String facilityType = "PHARMACY".equalsIgnoreCase(request.type()) ? "약국" : "병원";
        SearchArea area = reverseGeocode(request.latitude(), request.longitude());
        Place place = searchNearest(area, facilityType, request.latitude(), request.longitude());

        String appUrl = createNaverMapAppUrl(
                request.latitude(), request.longitude(), place.latitude(), place.longitude(), place.title()
        );
        String webUrl = createNaverMapWebUrl(
                request.latitude(), request.longitude(), place.latitude(), place.longitude(), place.title()
        );
        String ttsText = "주변 " + facilityType + " 검색 결과를 찾았어요. 목적지는 "
                + place.title() + "입니다. 네이버 지도를 열게요.";

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
                && Double.isFinite(request.latitude())
                && Double.isFinite(request.longitude())
                && request.latitude() >= -90 && request.latitude() <= 90
                && request.longitude() >= -180 && request.longitude() <= 180;
        if (!validType || !validCoordinates) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "의료시설 종류와 현재 위치를 올바르게 입력해야 합니다.",
                    "병원과 약국 중 어디를 찾을지 다시 말씀해 주세요."
            );
        }
        if (apiKeyId.isBlank() || apiKey.isBlank()) {
            throw new ApiException(
                    ErrorCode.GEOCODE_API_FAIL,
                    "NAVER_SEARCH_CLIENT_ID와 NAVER_SEARCH_CLIENT_SECRET이 설정되지 않았습니다.",
                    "지금은 이용할 수 없어요. 잠시 후 다시 해 주세요."
            );
        }
    }

    /** 현재 좌표를 지역 검색에 넣을 행정구역 이름으로 바꾼다. */
    private SearchArea reverseGeocode(double latitude, double longitude) {
        try {
            NominatimPlace place = reverseGeocodeClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("format", "json")
                            .queryParam("zoom", 14)
                            .queryParam("accept-language", "ko")
                            .build())
                    // Nominatim 이용 정책상 식별 가능한 User-Agent가 없으면 403으로 막힌다.
                    .header(HttpHeaders.USER_AGENT, appName)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .body(NominatimPlace.class);
            return toSearchArea(place == null ? null : place.displayName());
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new ApiException(
                    ErrorCode.GEOCODE_API_FAIL,
                    "현재 위치의 행정구역을 확인할 수 없습니다.",
                    "현재 위치를 확인하지 못했어요. 잠시 후 다시 해 주세요."
            );
        }
    }

    /**
     * Nominatim의 display_name은 작은 단위부터 나열된다.
     * "안양동, 만안구, 안양시, 경기도, 13997, 대한민국" -> "경기도 안양시 만안구 안양동".
     * 우편번호와 국가명은 검색어에 넣으면 방해만 되므로 버린다.
     */
    private SearchArea toSearchArea(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw locationNotFound();
        }
        List<String> names = new ArrayList<>(Arrays.stream(displayName.split(","))
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .filter(name -> !name.matches("\\d+"))
                .filter(name -> !"대한민국".equals(name))
                .toList());
        Collections.reverse(names);
        if (names.isEmpty()) {
            throw locationNotFound();
        }
        // 동 단위까지 붙인 상세 지역명으로 먼저 찾고, 결과가 없으면 시/군/구 단위로 넓힌다.
        return new SearchArea(join(names, 4), join(names, 2));
    }

    private String join(List<String> names, int limit) {
        return names.stream().limit(limit).collect(Collectors.joining(" "));
    }

    /** 지역 검색 결과 중 현재 위치에서 실제로 가장 가까운 한 곳을 고른다. */
    private Place searchNearest(
            SearchArea area,
            String facilityType,
            double latitude,
            double longitude
    ) {
        List<NaverSearchResponseDto.NaverSearchItem> items =
                searchNaverLocal(area.detailedName() + " " + facilityType);
        if (items.isEmpty() && !area.detailedName().equals(area.districtName())) {
            items = searchNaverLocal(area.districtName() + " " + facilityType);
        }
        return items.stream()
                .map(this::place)
                .min(Comparator.comparingDouble(place -> distanceKm(
                        latitude, longitude, place.latitude(), place.longitude()
                )))
                .orElseThrow(this::medicalFacilityNotFound);
    }

    private List<NaverSearchResponseDto.NaverSearchItem> searchNaverLocal(String query) {
        try {
            NaverSearchResponseDto response = localSearchClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/v1/local")
                            .queryParam("query", query)
                            // 지역 검색은 거리순 정렬을 지원하지 않으므로, 받을 수 있는 최대
                            // 개수를 받아 서버에서 직접 가장 가까운 곳을 고른다(최대 5건).
                            .queryParam("display", 5)
                            .queryParam("sort", "random")
                            .build())
                    .header("X-NCP-APIGW-API-KEY-ID", apiKeyId)
                    .header("X-NCP-APIGW-API-KEY", apiKey)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .body(NaverSearchResponseDto.class);
            return response == null || response.items() == null ? List.of() : response.items();
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new ApiException(
                    ErrorCode.GEOCODE_API_FAIL,
                    "네이버 의료시설 검색 결과를 처리할 수 없습니다.",
                    "지금은 병원이나 약국을 찾지 못했어요. 잠시 후 다시 해 주세요."
            );
        }
    }

    private Place place(NaverSearchResponseDto.NaverSearchItem item) {
        return new Place(
                cleanTitle(item.title()),
                coordinate(item.mapy(), 90),
                coordinate(item.mapx(), 180)
        );
    }

    private ApiException locationNotFound() {
        return new ApiException(
                ErrorCode.GEOCODE_NOT_FOUND,
                "현재 위치의 행정구역을 찾을 수 없습니다.",
                "현재 위치를 찾지 못했어요. 위치 권한을 확인해 주세요."
        );
    }

    private ApiException medicalFacilityNotFound() {
        return new ApiException(
                ErrorCode.GEOCODE_NOT_FOUND,
                "검색된 의료시설이 없습니다.",
                "주변 의료시설을 찾지 못했어요. 위치를 바꿔 다시 시도해 주세요."
        );
    }

    private double distanceKm(
            double startLatitude,
            double startLongitude,
            double goalLatitude,
            double goalLongitude
    ) {
        double latitudeDistance = Math.toRadians(goalLatitude - startLatitude);
        double longitudeDistance = Math.toRadians(goalLongitude - startLongitude);
        double haversine = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(Math.toRadians(startLatitude)) * Math.cos(Math.toRadians(goalLatitude))
                * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(haversine));
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
        } catch (NumberFormatException | NullPointerException exception) {
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

    /** 지역 검색에 넣을 지역명. 상세(동 단위)로 먼저 찾고 결과가 없으면 넓은 쪽으로 재시도한다. */
    private record SearchArea(String detailedName, String districtName) {}

    private record Place(String title, double latitude, double longitude) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimPlace(@JsonProperty("display_name") String displayName) {}
}
