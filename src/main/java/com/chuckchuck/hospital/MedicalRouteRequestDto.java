package com.chuckchuck.hospital;

public record MedicalRouteRequestDto(
        String userId,
        String text,
        String type,        // "HOSPITAL" 또는 "PHARMACY"
        Double latitude,
        Double longitude
) {}
