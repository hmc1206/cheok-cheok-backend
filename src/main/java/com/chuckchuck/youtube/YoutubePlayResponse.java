package com.chuckchuck.youtube;

import java.util.List;

public record YoutubePlayResponse(
        String intent,
        String step,
        Slots slots,
        String ttsText,
        String screen,
        List<QuickReply> quickReplies,
        Data data
) {

    public record Slots(
            String query
    ) {
    }

    public record QuickReply(
            String label,
            String value
    ) {
    }

    public record Data(
            String title,
            String thumbnailUrl,
            String channelName,
            String app_url,
            String web_url
    ) {
    }
}