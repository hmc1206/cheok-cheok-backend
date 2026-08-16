package com.chuckchuck.hospital;

import java.util.List;

public record NaverSearchResponseDto(
        String lastBuildDate,
        int total,
        int start,
        int display,
        List<NaverSearchItem> items
) {

    public record NaverSearchItem(
            String title,
            String link,
            String category,
            String description,
            String telephone,
            String address,
            String roadAddress,
            String mapx,
            String mapy
    ) {
    }
}