package com.chuckchuck.map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RouteStep(
        String type,
        String url
) {
    // 프론트엔드 응답 포맷이나 내부 변환 로직에서 App URL과 Web URL을 분리해 담아줄 수 있는 팩토리 메서드 예시입니다.
    public static RouteStep appUrl(String url) {
        return new RouteStep("APP_URL", url);
    }

    public static RouteStep webUrl(String url) {
        return new RouteStep("WEB_URL", url);
    }
}
        