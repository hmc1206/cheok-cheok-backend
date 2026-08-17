package com.chuckchuck.youtube;

import java.util.regex.Pattern;

/**
 * 검색 흐름과 재생 흐름이 같은 기준으로 "명령어를 뺀 검색어"만 뽑도록 한 곳에 모아둔다.
 * 두 핸들러가 각자 정규식을 들고 있으면 한쪽만 고쳐져 "아이유 검색해줘"와
 * "아이유 틀어줘"의 검색어가 달라지는 사고가 난다.
 */
final class YoutubeQuery {
    private static final Pattern COMMAND_WORDS = Pattern.compile(
            "유튜브(?:에서)?|동영상|영상|검색\\s*결과|검색(?:해)?\\s*줘|검색|"
                    + "찾아\\s*줘|찾아줘|찾아|틀어\\s*줘|틀어|재생해\\s*줘|재생해|재생|보여\\s*줘|보여"
    );

    private YoutubeQuery() {
    }

    static String extract(String userText) {
        return COMMAND_WORDS.matcher(userText).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }
}
