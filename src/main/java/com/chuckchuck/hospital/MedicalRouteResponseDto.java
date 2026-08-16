package com.chuckchuck.hospital;

public record MedicalRouteResponseDto(
        String intent,
        String step,
        String ttsText,
        String screen,
        RouteData data
) {
    public record RouteData(
            String naverMapAppUrl,
            String naverMapWebUrl
    ) {}
}
