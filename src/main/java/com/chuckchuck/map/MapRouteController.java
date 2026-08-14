package com.chuckchuck.map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes")
public class MapRouteController {

    private final NaverMapUrlService naverMapUrlService;

    public MapRouteController(NaverMapUrlService naverMapUrlService) {
        this.naverMapUrlService = naverMapUrlService;
    }

    @PostMapping("/naver-link")
    public ResponseEntity<NaverLinkResponse> createNaverLink(
            @RequestBody NaverLinkRequestDto requestDto) {

        // 서비스 로직을 호출하여 동적으로 네이버 링크 데이터를 생성합니다.
        NaverLinkResponse response = naverMapUrlService.generateRouteUrls(
                requestDto.getStartName(),
                requestDto.getGoalName()
        );

        return ResponseEntity.ok(response);
    }

    // --- 요구하신 API 스펙에 정확히 맞춘 응답 DTO 구조체 정의 ---
    public static class NaverLinkResponse {
        private int statusCode;
        private String message;
        private NaverLinkData data;

        public int getStatusCode() { return statusCode; }
        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public NaverLinkData getData() { return data; }
        public void setData(NaverLinkData data) { this.data = data; }
    }

    public static class NaverLinkData {
        private String naverMapAppUrl;
        private String naverMapWebUrl;

        public String getNaverMapAppUrl() { return naverMapAppUrl; }
        public void setNaverMapAppUrl(String naverMapAppUrl) { this.naverMapAppUrl = naverMapAppUrl; }
        public String getNaverMapWebUrl() { return naverMapWebUrl; }
        public void setNaverMapWebUrl(String naverMapWebUrl) { this.naverMapWebUrl = naverMapWebUrl; }
    }

    // 프론트엔드/포스트맨 요청 바디 매핑용 DTO
    public static class NaverLinkRequestDto {
        private String startName;
        private String goalName;

        public String getStartName() { return startName; }
        public void setStartName(String startName) { this.startName = startName; }
        public String getGoalName() { return goalName; }
        public void setGoalName(String goalName) { this.goalName = goalName; }
    }
}
