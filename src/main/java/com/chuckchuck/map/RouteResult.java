package com.chuckchuck.map;

// 새로운 네이버 길찾기 링크 API 응답 데이터를 담기 위한 레코드 구조로 변경합니다.
public record RouteResult(
        String naverMapAppUrl,
        String naverMapWebUrl
) {
}
