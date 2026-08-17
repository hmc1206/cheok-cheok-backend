package com.chuckchuck.youtube;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.chuckchuck.voice.QuickReply;
import org.springframework.stereotype.Component;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.IntentHandler;
import com.chuckchuck.voice.VoiceResponse;

@Component
public class YoutubeIntentHandler implements IntentHandler {
    private static final String CONFIRM = "CONFIRM";
    private static final String DONE = "DONE";
    private static final String SCREEN = "APP_LAUNCH";
    private static final String SEARCH = "SEARCH";
    private static final String PLAY = "PLAY";

    private static final Pattern SEARCH_WORDS = Pattern.compile("검색|찾아");
    private static final Pattern PLAY_WORDS = Pattern.compile("틀어|재생|보여");

    private static final Pattern COMMAND_WORDS = Pattern.compile(
            "유튜브(?:에서)?|동영상|영상|검색\\s*결과|검색(?:해)?\\s*줘|검색|"
                    + "찾아\\s*줘|찾아줘|찾아|틀어\\s*줘|틀어|재생해\\s*줘|재생해|재생|보여\\s*줘|보여"
    );

    private final YoutubeApiClient youtubeApiClient;
    private final YoutubeLinkBuilder linkBuilder;

    public YoutubeIntentHandler(YoutubeApiClient youtubeApiClient, YoutubeLinkBuilder linkBuilder) {
        this.youtubeApiClient = youtubeApiClient;
        this.linkBuilder = linkBuilder;
    }

    @Override
    public Intent supports() {
        return Intent.YOUTUBE_PLAY;
    }

    @Override
    public VoiceResponse handle(SessionState session, String userText) {
        Map<String, Object> slots = new LinkedHashMap<>(session.slots());

        if (CONFIRM.equals(session.step())) {
            return confirm(slots, userText);
        }
        return isSearchOnly(userText)
                ? showSearchResults(slots, userText)
                : preparePlay(slots, userText);
    }

    private VoiceResponse showSearchResults(Map<String, Object> slots, String userText) {
        String query = extractQuery(userText);
        slots.put("query", query);
        slots.put("action", SEARCH);

        if (query.isBlank()) {
            return done(slots, "검색할 영상을 다시 말씀해 주세요.", null);
        }

        return done(
                slots,
                query + " 검색 결과를 보여드릴게요.",
                YoutubeLinkResult.linkOnly(linkBuilder.forSearch(query))
        );
    }

    private VoiceResponse preparePlay(Map<String, Object> slots, String userText) {
        String query = extractQuery(userText);
        slots.put("query", query);
        slots.put("action", PLAY);

        if (query.isBlank()) {
            return done(slots, "어떤 영상을 틀어드릴지 다시 말씀해 주세요.", null);
        }

        return youtubeApiClient.searchFirst(query)
                .map(video -> {
                    slots.put("videoId", video.videoId());
                    var links = linkBuilder.forVideo(video.videoId());
                    return new VoiceResponse(
                            Intent.YOUTUBE_PLAY, CONFIRM, slots,
                            query + " 영상을 열어드릴까요?",
                            SCREEN,
                            List.of(new QuickReply("네, 열어줘", "네"), new QuickReply("아니요", "아니요")),
                            YoutubeLinkResult.preview(video, links)
                    );
                })
                .orElseGet(() -> {
                    slots.put("action", SEARCH);
                    return done(slots, "해당 영상을 찾지 못했어요. 유튜브 검색 결과를 보여드릴게요.",
                            YoutubeLinkResult.linkOnly(linkBuilder.forSearch(query)));
                });
    }

    private VoiceResponse confirm(Map<String, Object> slots, String userText) {
        String text = userText.trim();
        boolean positive = text.contains("네") || text.contains("예") || text.contains("응");

        if (!positive) {
            return done(slots, "알겠어요. 다음에 다시 말씀해 주세요.", null);
        }

        String videoId = String.valueOf(slots.get("videoId"));
        var links = linkBuilder.forVideo(videoId);
        return done(slots, "유튜브를 열어드릴게요.", YoutubeLinkResult.linkOnly(links));
    }

    private VoiceResponse done(Map<String, Object> slots, String ttsText, Object data) {
        return new VoiceResponse(Intent.YOUTUBE_PLAY, DONE, slots, ttsText, SCREEN, data);
    }

    String extractQuery(String userText) {
        return COMMAND_WORDS.matcher(userText).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    private boolean isSearchOnly(String userText) {
        return SEARCH_WORDS.matcher(userText).find() && !PLAY_WORDS.matcher(userText).find();
    }
}
