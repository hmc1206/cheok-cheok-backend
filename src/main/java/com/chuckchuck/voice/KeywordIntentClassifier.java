package com.chuckchuck.voice;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class KeywordIntentClassifier implements IntentClassifier {

    @Override
    public Intent classify(String userText) {
        String text = userText.toLowerCase(Locale.ROOT);

        // 구체적인 도메인 단어를 먼저 검사해 "부산 가는 기차" 같은 문장을 길찾기로 오분류하지 않는다.
        if (containsAny(text, "기차", "열차", "ktx", "기차표", "예매")) {
            return Intent.TRAIN_BOOKING;
        }
        if (containsAny(text, "유튜브", "영상", "동영상", "틀어줘", "재생")) {
            return Intent.YOUTUBE_PLAY;
        }
        if (containsAny(text, "키오스크", "주문 연습", "무인 주문")) {
            return Intent.KIOSK_TRAIN;
        }
        if (containsAny(text, "길 알려", "길찾기", "지도", "경로", "가는 길")) {
            return Intent.MAP_ROUTE;
        }
        return Intent.UNKNOWN;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
