package com.chuckchuck.youtube;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.IntentHandler;
import com.chuckchuck.voice.VoiceResponse;

/**
 * 프론트 "영상 도움 - 01 검색하기" 흐름.
 *
 * 프론트(YoutubePlayerScreen)는 intent가 YOUTUBE_SEARCH이고 step이 CONFIRM일 때만 영상 목록
 * 화면을 그린다. 그래서 검색에 성공하면 DONE이 아니라 CONFIRM으로 목록을 내려준다.
 * 목록에서 영상을 고른 뒤의 딥링크는 프론트가 videoId로 직접 조립하므로 서버가 다시 응답할
 * 필요가 없다 - 재생(YOUTUBE_PLAY) 흐름과 다른 점이 이것이다.
 */
@Component
public class YoutubeSearchIntentHandler implements IntentHandler {
    private static final String CONFIRM = "CONFIRM";
    private static final String DONE = "DONE";
    private static final String SCREEN = "YOUTUBE_SEARCH_RESULT";
    private static final Pattern BARE_ANSWER = Pattern.compile("^(네|예|응|아니|아니요|아니야)[.!?]?$");

    private final YoutubeApiClient youtubeApiClient;

    public YoutubeSearchIntentHandler(YoutubeApiClient youtubeApiClient) {
        this.youtubeApiClient = youtubeApiClient;
    }

    @Override
    public Intent supports() {
        return Intent.YOUTUBE_SEARCH;
    }

    @Override
    public VoiceResponse handle(SessionState session, String userText) {
        Map<String, Object> slots = new LinkedHashMap<>(session.slots());

        // 목록을 이미 보여준 뒤(CONFIRM) 사용자가 "네"라고만 답하는 경우. 프론트는 이 흐름에서
        // 서버로 확인 응답을 보내지 않으므로 음성으로만 들어올 수 있는데, 그 말을 검색어로 삼아
        // 다시 검색하면 "네"를 찾아버린다. 여기서 DONE으로 끝내 세션도 함께 정리한다.
        if (CONFIRM.equals(session.step()) && BARE_ANSWER.matcher(userText.trim()).matches()) {
            return done(slots, "목록에서 보고 싶은 영상을 골라 주세요.", null);
        }

        String query = YoutubeQuery.extract(userText);
        slots.put("query", query);

        if (query.isBlank()) {
            return done(slots, "어떤 영상을 찾아드릴지 다시 말씀해 주세요.", null);
        }

        List<YoutubeVideoResponse> videos = youtubeApiClient.search(query);
        if (videos.isEmpty()) {
            // 빈 목록을 CONFIRM으로 내려주면 프론트가 빈 목록 화면에 머무르고 세션도 남는다.
            // 다시 말할 수 있게 DONE으로 끝낸다.
            return done(slots, query + " 검색 결과가 없어요. 다른 말로 다시 말씀해 주세요.", new SearchResults(videos));
        }

        return new VoiceResponse(
                Intent.YOUTUBE_SEARCH,
                CONFIRM,
                slots,
                query + " 검색 결과예요. 보고 싶은 영상을 골라 주세요.",
                SCREEN,
                new SearchResults(videos)
        );
    }

    private VoiceResponse done(Map<String, Object> slots, String ttsText, Object data) {
        return new VoiceResponse(Intent.YOUTUBE_SEARCH, DONE, slots, ttsText, SCREEN, data);
    }

    // 프론트의 extractSearchResults는 배열 그대로도, results/items/videos/list 키로 감싼
    // 객체도 받는다. 다른 intent처럼 data를 객체로 유지하려고 videos 키로 감싼다.
    record SearchResults(List<YoutubeVideoResponse> videos) {
    }
}
