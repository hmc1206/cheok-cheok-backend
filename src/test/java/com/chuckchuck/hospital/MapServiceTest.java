package com.chuckchuck.hospital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

class MapServiceTest {
    // 안양시 만안구 근처 좌표. 네이버 지역 검색이 좌표를 무시하는 탓에 예전에는 이 좌표로도
    // 서울 병원이 나왔다 - 그래서 "좌표 -> 지역명 -> 가장 가까운 곳"이 이 테스트의 핵심이다.
    private static final double LATITUDE = 37.3799;
    private static final double LONGITUDE = 126.9284;
    private static final String ANYANG_DISPLAY_NAME =
            "{\"display_name\": \"안양동, 만안구, 안양시, 경기도, 13997, 대한민국\"}";

    private MockRestServiceServer server;
    private MapService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new MapService(
                builder,
                "test-client-id",
                "test-client-secret",
                "com.chuckchuck.app"
        );
    }

    @Test
    void searchesCurrentAreaAndSelectsNearestFacility() {
        server.expect(requestTo(containsString("/reverse")))
                .andRespond(withSuccess(ANYANG_DISPLAY_NAME, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/search/v1/local")))
                .andExpect(header("X-NCP-APIGW-API-KEY-ID", "test-client-id"))
                .andExpect(header("X-NCP-APIGW-API-KEY", "test-client-secret"))
                // 현재 위치의 행정구역이 검색어에 들어가야 서울 결과가 나오지 않는다.
                .andExpect(queryParam(
                        "query",
                        UriUtils.encodeQueryParam("경기도 안양시 만안구 안양동 병원", StandardCharsets.UTF_8)
                ))
                .andExpect(queryParam("display", "5"))
                .andRespond(withSuccess("""
                        {
                          "items": [{
                            "title": "먼병원",
                            "roadAddress": "경기도 안양시 만안구",
                            "mapx": "1269284000",
                            "mapy": "373999000"
                          }, {
                            "title": "<b>가까운병원</b>",
                            "roadAddress": "경기도 안양시 만안구",
                            "mapx": "1269290000",
                            "mapy": "373810000"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        MedicalRouteResponseDto response = service.processMedicalRoute(
                new MedicalRouteRequestDto("u123", "병원 찾아줘", "HOSPITAL", LATITUDE, LONGITUDE)
        );

        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.screen()).isEqualTo("NAVER_MAP_VIEW");
        // 첫 번째 결과가 아니라 실제로 더 가까운 곳을 골라야 한다.
        assertThat(response.ttsText()).contains("병원", "가까운병원", "네이버 지도");
        assertThat(response.data().naverMapAppUrl())
                .contains("slat=37.3799", "slng=126.9284", "dlat=37.381", "dlng=126.929");
        assertThat(response.data().naverMapWebUrl()).contains("126.929,37.381");
        server.verify();
    }

    @Test
    void widensToDistrictWhenDetailedAreaHasNoResult() {
        server.expect(requestTo(containsString("/reverse")))
                .andRespond(withSuccess(ANYANG_DISPLAY_NAME, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/search/v1/local")))
                .andExpect(queryParam(
                        "query",
                        UriUtils.encodeQueryParam("경기도 안양시 만안구 안양동 약국", StandardCharsets.UTF_8)
                ))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/search/v1/local")))
                .andExpect(queryParam(
                        "query",
                        UriUtils.encodeQueryParam("경기도 안양시 약국", StandardCharsets.UTF_8)
                ))
                .andRespond(withSuccess("""
                        {
                          "items": [{
                            "title": "안양약국",
                            "roadAddress": "경기도 안양시",
                            "mapx": "1269290000",
                            "mapy": "373810000"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        MedicalRouteResponseDto response = service.processMedicalRoute(
                new MedicalRouteRequestDto("u123", "약국 찾아줘", "PHARMACY", LATITUDE, LONGITUDE)
        );

        assertThat(response.ttsText()).contains("약국", "안양약국");
        server.verify();
    }

    @Test
    void returnsTtsFriendlyNotFoundError() {
        server.expect(requestTo(containsString("/reverse")))
                .andRespond(withSuccess(ANYANG_DISPLAY_NAME, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/search/v1/local")))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/search/v1/local")))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.processMedicalRoute(
                new MedicalRouteRequestDto("u123", "병원 찾아줘", "HOSPITAL", LATITUDE, LONGITUDE)
        )).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.errorCode()).isEqualTo(ErrorCode.GEOCODE_NOT_FOUND);
            assertThat(exception.ttsText()).contains("의료시설을 찾지 못했어요");
        });
    }

    @Test
    void rejectsMissingCoordinates() {
        assertThatThrownBy(() -> service.processMedicalRoute(
                new MedicalRouteRequestDto("u123", "병원 찾아줘", "HOSPITAL", null, null)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }
}
