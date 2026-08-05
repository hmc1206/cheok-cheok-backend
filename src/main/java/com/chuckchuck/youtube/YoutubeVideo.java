package com.chuckchuck.youtube;

public record YoutubeVideo(
        String videoId,
        String title,
        String thumbnailUrl,
        String channelName,
        int durationSeconds
) {
}
