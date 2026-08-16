package com.chuckchuck.hospital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

class MapServiceTest {
    private MockRestServiceServer server;
    private MapService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new MapService(
                builder,
                new ObjectMapper(),
                "test-client-id",
                "test-client-secret",
                "com.chuckchuck.app"
        );
    }

    @Test
    void convertsNaverCoordinatesAndBuildsTtsRoute() {
        server.expect(requestTo(containsString("/search/v1/local")))
                .andExpect(header("X-NCP-APIGW-API-KEY-ID", "test-client-id"))
                .andExpect(header("X-NCP-APIGW-API-KEY", "test-client-secret"))
                .andRespond(withSuccess("""
                        {
                          "items": [{
                            "title": "<b>새봄약국</b>",
                            "roadAddress": "서울특별시 종로구",
                            "mapx": "1269779680",
                            "mapy": "375679455"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        MedicalRouteResponseDto response = service.processMedicalRoute(
                new MedicalRouteRequestDto("u123", "약국 찾아줘", "PHARMACY", 37.5665, 126.978)
        );

        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.screen()).isEqualTo("NAVER_MAP_VIEW");
        assertThat(response.ttsText()).contains("약국", "새봄약국", "네이버 지도");
        assertThat(response.data().naverMapAppUrl()).contains("dlat=37.5679455", "dlng=126.977968");
        assertThat(response.data().naverMapWebUrl()).contains("126.977968,37.5679455");
        server.verify();
    }

    @Test
    void returnsTtsFriendlyNotFoundError() {
        server.expect(requestTo(containsString("/search/v1/local")))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.processMedicalRoute(
                new MedicalRouteRequestDto("u123", "병원 찾아줘", "HOSPITAL", 37.5665, 126.978)
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
