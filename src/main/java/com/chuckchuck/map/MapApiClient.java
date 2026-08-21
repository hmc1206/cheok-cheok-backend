//package com.chuckchuck.map;
//
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//import java.util.Optional;
//
//@Component
//public class MapApiClient {
//
//    private final RestTemplate restTemplate;
//    // 실제 외부 백엔드 서버의 도메인(IP/Port 포함) 주소를 기입하세요.
//    private final String serverUrl = "http://localhost:8080/api/v1/routes/naver-link";
//
//    public MapApiClient() {
//        this.restTemplate = new RestTemplate();
//    }
//
//    public Optional<NaverLinkResponse> findRoute(String origin, String destination) {
//        // 1. 요청 헤더 및 바디 데이터 생성
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        NaverLinkRequest requestBody = new NaverLinkRequest(origin, destination);
//        HttpEntity<NaverLinkRequest> entity = new HttpEntity<>(requestBody, headers);
//
//        try {
//            // 2. POST 요청 실행 및 응답 매핑
//            NaverLinkResponse response = restTemplate.postForObject(serverUrl, entity, NaverLinkResponse.class);
//            return Optional.ofNullable(response);
//        } catch (Exception e) {
//            return Optional.empty();
//        }
//    }
//
//    // --- API 스펙에 맞춘 내부 DTO 클래스들 ---
//    public static class NaverLinkRequest {
//        private final String startName;
//        private final String goalName;
//
//        public NaverLinkRequest(String startName, String goalName) {
//            this.startName = startName;
//            this.goalName = goalName;
//        }
//
//        public String getStartName() { return startName; }
//        public String getGoalName() { return goalName; }
//    }
//
//    public static class NaverLinkResponse {
//        private int statusCode;
//        private String message;
//        private NaverLinkData data;
//
//        public int getStatusCode() { return statusCode; }
//        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
//        public String getMessage() { return message; }
//        public void setMessage(String message) { this.message = message; }
//        public NaverLinkData getData() { return data; }
//        public void setData(NaverLinkData data) { this.data = data; }
//    }
//
//    public static class NaverLinkData {
//        private String naverMapAppUrl;
//        private String naverMapWebUrl;
//
//        public String getNaverMapAppUrl() { return naverMapAppUrl; }
//        public void setNaverMapAppUrl(String naverMapAppUrl) { this.naverMapAppUrl = naverMapAppUrl; }
//        public String getNaverMapWebUrl() { return naverMapWebUrl; }
//        public void setNaverMapWebUrl(String naverMapWebUrl) { this.naverMapWebUrl = naverMapWebUrl; }
//    }
//}
