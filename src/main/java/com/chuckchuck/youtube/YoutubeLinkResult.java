package com.chuckchuck.youtube;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record YoutubeLinkResult(
        String title,
        String thumbnailUrl,
        String channelName,
        @JsonProperty("app_url") String appUrl,
        @JsonProperty("web_url") String webUrl
) {
    // CONFIRM 단계: 미리보기 정보 포함
    public static YoutubeLinkResult preview(YoutubeVideo video, YoutubeLinkBuilder.YoutubeLinks links) {
        return new YoutubeLinkResult(video.title(), video.thumbnailUrl(), video.channelName(), links.appUrl(), links.webUrl());
    }

    // DONE 단계: 링크만 포함
    public static YoutubeLinkResult linkOnly(YoutubeLinkBuilder.YoutubeLinks links) {
        return new YoutubeLinkResult(null, null, null, links.appUrl(), links.webUrl());
    }
}