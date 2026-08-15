package com.chuckchuck.map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
public class NaverMapUrlService {

    private static final String LOCAL_SEARCH_URL =
            "https://naverapihub.apigw.ntruss.com/search/v1/local";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final String clientId;
    private final String clientSecret;
    private final String appName;

    public NaverMapUrlService(
            @Value("${naver.search.client-id}") String clientId,
            @Value("${naver.search.client-secret}") String clientSecret,
            @Value("${app.naver-map.app-name:com.chuckchuck.app}") String appName
    ) {
        this.clientId = clientId.trim();
        this.clientSecret = clientSecret.trim();
        this.appName = appName.trim();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    /**
     * 출발지와 목적지를 검색하여
     * 네이버 지도 앱 / 웹 길찾기 URL을 생성한다.
     */
    public MapRouteController.NaverLinkResponse generateRouteUrls(
            String startName,
            String goalName
    ) {

        if (startName == null || startName.isBlank()) {
            throw new IllegalArgumentException("출발지가 비어 있습니다.");
        }

        if (goalName == null || goalName.isBlank()) {
            throw new IllegalArgumentException("도착지가 비어 있습니다.");
        }

        startName = startName.trim();
        goalName = goalName.trim();

        System.out.println("\n=== [네이버 길찾기 시작] ===");
        System.out.println("[출발지] " + startName);
        System.out.println("[도착지] " + goalName);

        /*
         * 1. 출발지 검색
         */
        Location start = searchLocation(startName);

        /*
         * 2. 도착지 검색
         */
        Location goal = searchLocation(goalName);

        /*
         * 3. 장소명을 URL 인코딩
         */
        String encodedStartName = encode(startName);
        String encodedGoalName = encode(goalName);
        String encodedAppName = encode(appName);

        /*
         * 4. 네이버 지도 앱 딥링크
         *
         * slat = 출발 위도
         * slng = 출발 경도
         * dlat = 도착 위도
         * dlng = 도착 경도
         */
        String appUrl = String.format(
                "nmap://route/public" +
                        "?slat=%s" +
                        "&slng=%s" +
                        "&sname=%s" +
                        "&dlat=%s" +
                        "&dlng=%s" +
                        "&dname=%s" +
                        "&appname=%s",
                start.latitude(),
                start.longitude(),
                encodedStartName,
                goal.latitude(),
                goal.longitude(),
                encodedGoalName,
                encodedAppName
        );

        /*
         * 5. 네이버 지도 웹 길찾기
         *
         * 형식:
         *
         * /p/directions/
         * 출발경도,출발위도,출발지명/
         * 도착경도,도착위도,도착지명/
         * -/public
         */
        String webUrl = String.format(
                "https://map.naver.com/p/directions/" +
                        "%s,%s,%s/" +
                        "%s,%s,%s/-/public",
                start.longitude(),
                start.latitude(),
                encodedStartName,
                goal.longitude(),
                goal.latitude(),
                encodedGoalName
        );

        System.out.println("\n[검색 결과]");

        System.out.println(
                "출발지 주소: " +
                        start.roadAddress()
        );

        System.out.println(
                "출발지 좌표: " +
                        start.latitude() +
                        ", " +
                        start.longitude()
        );

        System.out.println(
                "도착지 주소: " +
                        goal.roadAddress()
        );

        System.out.println(
                "도착지 좌표: " +
                        goal.latitude() +
                        ", " +
                        goal.longitude()
        );

        System.out.println("\n[최종 URL]");
        System.out.println("APP: " + appUrl);
        System.out.println("WEB: " + webUrl);
        System.out.println("==============================\n");

        /*
         * 6. Response 생성
         */
        MapRouteController.NaverLinkResponse response =
                new MapRouteController.NaverLinkResponse();

        response.setStatusCode(200);
        response.setMessage(
                "네이버 장소 검색 및 길찾기 링크 생성이 완료되었습니다."
        );

        MapRouteController.NaverLinkData data =
                new MapRouteController.NaverLinkData();

        data.setNaverMapAppUrl(appUrl);
        data.setNaverMapWebUrl(webUrl);

        response.setData(data);

        return response;
    }

    /**
     * NAVER Local Search API
     *
     * 장소명
     * ↓
     * 검색 결과
     * ↓
     * 도로명 주소 + 좌표
     */
    private Location searchLocation(String keyword) {

        System.out.println("-------------------------------------------");
        System.out.println("[NAVER 장소 검색] " + keyword);

        try {

            String encodedQuery = encode(keyword);

            String url =
                    LOCAL_SEARCH_URL +
                            "?query=" + encodedQuery +
                            "&display=1" +
                            "&start=1" +
                            "&sort=random" +
                            "&format=json";

            URI uri = URI.create(url);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(uri)
                            .timeout(Duration.ofSeconds(5))
                            .header(
                                    "X-NCP-APIGW-API-KEY-ID",
                                    clientId
                            )
                            .header(
                                    "X-NCP-APIGW-API-KEY",
                                    clientSecret
                            )
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            System.out.println(
                    "[NAVER 장소 검색] HTTP Status: " +
                            response.statusCode()
            );

            if (response.statusCode() != 200) {

                System.err.println(
                        "[NAVER 장소 검색] API 호출 실패"
                );

                System.err.println(
                        response.body()
                );

                throw new IllegalStateException(
                        "NAVER 장소 검색 API 호출에 실패했습니다."
                );
            }

            /*
             * JSON → DTO
             */
            NaverSearchResponse searchResponse =
                    objectMapper.readValue(
                            response.body(),
                            NaverSearchResponse.class
                    );

            /*
             * 검색 결과가 없는 경우
             */
            if (searchResponse.items() == null
                    || searchResponse.items().isEmpty()) {

                throw new IllegalArgumentException(
                        "검색 결과가 없습니다: " + keyword
                );
            }

            /*
             * 첫 번째 검색 결과 사용
             */
            SearchItem item =
                    searchResponse.items().get(0);

            /*
             * 도로명 주소가 없으면 일반 주소 사용
             */
            String roadAddress = item.roadAddress();

            if (roadAddress == null || roadAddress.isBlank()) {
                roadAddress = item.address();
            }

            if (roadAddress == null || roadAddress.isBlank()) {

                throw new IllegalArgumentException(
                        "주소를 찾을 수 없습니다: " + keyword
                );
            }

            /*
             * NAVER Search API의 mapx/mapy는
             * 소수점이 없는 좌표 형태일 수 있으므로 변환한다.
             *
             * 예:
             *
             * mapx = 1269997956
             * mapy = 372660447
             *
             * ↓
             *
             * 126.9997956
             * 37.2660447
             */
            String longitude =
                    convertCoordinate(item.mapx());

            String latitude =
                    convertCoordinate(item.mapy());

            System.out.println(
                    "[NAVER 장소 검색] 검색 성공"
            );

            System.out.println(
                    "  장소명: " +
                            removeHtml(item.title())
            );

            System.out.println(
                    "  도로명 주소: " +
                            roadAddress
            );

            System.out.println(
                    "  위도: " +
                            latitude
            );

            System.out.println(
                    "  경도: " +
                            longitude
            );

            return new Location(
                    roadAddress,
                    latitude,
                    longitude
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "NAVER 장소 검색 요청이 중단되었습니다.",
                    e
            );

        } catch (Exception e) {

            System.err.println(
                    "[NAVER 장소 검색] 예외 발생: " +
                            e.getMessage()
            );

            throw new IllegalStateException(
                    "장소 검색에 실패했습니다: " + keyword,
                    e
            );
        }
    }

    /**
     * NAVER Search API 좌표 변환
     *
     * 1269997956
     * ↓
     * 126.9997956
     */
    private String convertCoordinate(String coordinate) {

        if (coordinate == null || coordinate.isBlank()) {
            throw new IllegalArgumentException(
                    "좌표가 비어 있습니다."
            );
        }

        if (coordinate.contains(".")) {
            return coordinate;
        }

        if (coordinate.length() <= 1) {
            return coordinate;
        }

        int decimalPosition =
                coordinate.length() - 7;

        return coordinate.substring(
                0,
                decimalPosition
        ) + "." +
                coordinate.substring(decimalPosition);
    }

    /**
     * URL 인코딩
     */
    private String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    /**
     * NAVER 검색 결과 title에는
     * <b> 같은 HTML 태그가 포함될 수 있다.
     */
    private String removeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replaceAll("<[^>]*>", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    /*
     * 검색 결과에서 실제로 사용할 데이터
     */
    private record Location(
            String roadAddress,
            String latitude,
            String longitude
    ) {
    }

    /*
     * NAVER Local Search API 응답
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverSearchResponse(
            List<SearchItem> items
    ) {
    }

    /*
     * 검색 결과 한 건
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchItem(
            String title,
            String address,
            String roadAddress,
            String mapx,
            String mapy
    ) {
    }
}