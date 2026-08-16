package com.chuckchuck.hospital;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MapService {

    @Value("${naver.search.client-id}")
    private String apiKeyId;

    @Value("${naver.search.client-secret}")
    private String apiKey;

    private final ObjectMapper objectMapper;

    private final RestClient restClient = RestClient.builder().build();


    /**
     * 의료시설 검색 → 첫 번째 결과 선택 → 네이버 지도 길찾기 URL 생성
     */
    public MedicalRouteResponseDto processMedicalRoute(
            MedicalRouteRequestDto request
    ) {

        // ========================================
        // 1. 현재 위치
        // ========================================

        double startLatitude = request.latitude();
        double startLongitude = request.longitude();


        // ========================================
        // 2. 검색어 결정
        // ========================================

        String query;

        if ("PHARMACY".equalsIgnoreCase(request.type())) {
            query = "약국";
        } else {
            query = "병원";
        }


        // ========================================
        // 3. 네이버 Local API 검색
        // ========================================

        JsonNode place = searchNaverLocal(
                query,
                startLatitude,
                startLongitude
        );


        // ========================================
        // 4. 첫 번째 검색 결과 정보 추출
        // ========================================

        String title = cleanTitle(
                place.path("title").asText()
        );

        String address = place.path("roadAddress").asText();

        String mapx = place.path("mapx").asText();

        String mapy = place.path("mapy").asText();


        System.out.println("========================================");
        System.out.println("검색된 의료시설");
        System.out.println("이름 = " + title);
        System.out.println("주소 = " + address);
        System.out.println("mapx = " + mapx);
        System.out.println("mapy = " + mapy);
        System.out.println("========================================");


        // ========================================
        // 5. 현재 단계에서는 mapx/mapy를
        //    그대로 네이버 지도 URL에 사용
        // ========================================

        double goalLongitude;

        double goalLatitude;

        try {

            goalLongitude = Double.parseDouble(mapx);

            goalLatitude = Double.parseDouble(mapy);

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "검색된 의료시설의 좌표를 읽을 수 없습니다."
            );
        }


        // ========================================
        // 6. 네이버 지도 길찾기 URL 생성
        // ========================================

        String webUrl = createNaverMapDirectionUrl(
                startLatitude,
                startLongitude,
                "현재 위치",
                goalLatitude,
                goalLongitude,
                title
        );


        // ========================================
        // 7. TTS
        // ========================================

        String facilityType =
                "PHARMACY".equalsIgnoreCase(request.type())
                        ? "약국"
                        : "병원";

        String ttsText = String.format(
                "가까운 %s %s을 찾았습니다. 길찾기를 시작합니다.",
                title,
                facilityType
        );


        // ========================================
        // 8. Response
        // ========================================

        return new MedicalRouteResponseDto(
                "MEDICAL_ROUTE",
                "COMPLETE",
                ttsText,
                "NAVER_MAP",
                new MedicalRouteResponseDto.RouteData(
                        webUrl,
                        webUrl
                )
        );
    }


    /**
     * ========================================
     * 네이버 Local API 검색
     * ========================================
     */
    private JsonNode searchNaverLocal(
            String query,
            double latitude,
            double longitude
    ) {

        try {

            ResponseEntity<String> response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("naverapihub.apigw.ntruss.com")
                            .path("/search/v1/local")
                            .queryParam("query", query)
                            .queryParam("display", 1)
                            .queryParam("sort", "random")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .build()
                    )
                    .header(
                            "X-NCP-APIGW-API-KEY-ID",
                            apiKeyId
                    )
                    .header(
                            "X-NCP-APIGW-API-KEY",
                            apiKey
                    )
                    .retrieve()
                    .toEntity(String.class);


            // ========================================
            // 네이버 실제 응답 확인
            // ========================================

            System.out.println("========================================");
            System.out.println("NAVER API RESPONSE");
            System.out.println(response.getBody());
            System.out.println("========================================");


            // ========================================
            // JSON 파싱
            // ========================================

            JsonNode root = objectMapper.readTree(
                    response.getBody()
            );


            // ========================================
            // items 확인
            // ========================================

            JsonNode items = root.path("items");


            if (!items.isArray() || items.isEmpty()) {

                throw new IllegalArgumentException(
                        "검색된 의료시설이 없습니다."
                );
            }


            // ========================================
            // 첫 번째 검색 결과만 사용
            // ========================================

            return items.get(0);

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "네이버 의료시설 검색 결과를 처리할 수 없습니다.",
                    e
            );
        }
    }


    /**
     * ========================================
     * 네이버 지도 길찾기 URL
     * ========================================
     */
    private String createNaverMapDirectionUrl(
            double startLatitude,
            double startLongitude,
            String startName,
            double goalLatitude,
            double goalLongitude,
            String goalName
    ) {

        String encodedStartName = URLEncoder.encode(
                startName,
                StandardCharsets.UTF_8
        );

        String encodedGoalName = URLEncoder.encode(
                goalName,
                StandardCharsets.UTF_8
        );


        return String.format(
                "https://map.naver.com/p/directions/" +
                        "%s,%s,%s/" +
                        "%s,%s,%s/-/public",

                startLongitude,
                startLatitude,
                encodedStartName,

                goalLongitude,
                goalLatitude,
                encodedGoalName
        );
    }


    /**
     * ========================================
     * 네이버 검색 결과의 <b> 태그 제거
     * ========================================
     *
     * 예:
     *
     * <b>명약국</b>
     *
     * ↓
     *
     * 명약국
     */
    private String cleanTitle(String title) {

        if (title == null || title.isBlank()) {
            return "의료시설";
        }

        return title
                .replaceAll("<[^>]*>", "")
                .trim();
    }
}