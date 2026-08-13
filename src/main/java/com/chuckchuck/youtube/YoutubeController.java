package com.chuckchuck.youtube;

import java.util.Locale;

import org.springframework.web.bind.annotation.*;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

@RestController
@RequestMapping("/api/youtube")
public class YoutubeController {


    private final YoutubeLinkBuilder linkBuilder;

    public YoutubeController(YoutubeLinkBuilder linkBuilder) {
        this.linkBuilder = linkBuilder;
    }

    // 통합 API명세서 8-1: GET /api/v1/youtube/link
    @GetMapping("/link")
    public YoutubeLinkApiResponse link(@RequestParam(required = false) String keyword) {
        var links = (keyword == null || keyword.isBlank())
                ? new YoutubeLinkBuilder.YoutubeLinks("vnd.youtube://www.youtube.com", "https://www.youtube.com")
                : linkBuilder.forSearch(keyword);
        return new YoutubeLinkApiResponse(true, new YoutubeLinkApiResponse.Data(links.webUrl(), links.appUrl()));
    }

    public record YoutubeLinkApiResponse(boolean success, Data data) {
        public record Data(String web_url, String app_url) {
        }
    }
}
