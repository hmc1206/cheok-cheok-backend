package com.chuckchuck.youtube;

import java.util.List;

public record YoutubeSearchListResponse(
        String intent,
        String step,
        Slots slots,
        String ttsText,
        String screen,
        Data data
) {
    public record Slots(String keyword) {}
    public record Data(List<YoutubeVideoResponse> videos) {} // 또는 내부 리스트 구조에 맞게 명칭 조절
}
