package com.chuckchuck.hospital;

import java.util.List;

public record NaverReverseGeocodeResponseDto(
        List<Result> results
) {

    public record Result(
            Region region
    ) {
    }

    public record Region(
            Area area1,
            Area area2,
            Area area3,
            Area area4
    ) {
    }

    public record Area(
            Name name
    ) {
    }

    public record Name(
            String text
    ) {
    }
}