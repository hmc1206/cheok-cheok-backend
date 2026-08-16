package com.chuckchuck.hospital;

public record MedicalRouteRequestDto(
        String userId,
        String text,
        String type,        // "HOSPITAL" 또는 "PHARMACY"
        double latitude,    // 🌟 추가: 출발지 위도 (예: 37.5665)
        double longitude    // 🌟 추가: 출발지 경도 (예: 126.9780)
) {}
