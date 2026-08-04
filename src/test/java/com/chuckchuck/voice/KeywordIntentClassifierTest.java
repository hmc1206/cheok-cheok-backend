package com.chuckchuck.voice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class KeywordIntentClassifierTest {
    private final KeywordIntentClassifier classifier = new KeywordIntentClassifier();

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            부산 가는 기차표 끊어줘 | TRAIN_BOOKING
            미스트롯 영상 틀어줘 | YOUTUBE_PLAY
            키오스크 주문 연습할래 | KIOSK_TRAIN
            아들 집 가는 길 알려줘 | MAP_ROUTE
            """)
    void classifiesSupportedIntents(String text, Intent expected) {
        assertThat(classifier.classify(text)).isEqualTo(expected);
    }

    @Test
    void returnsUnknownForUnsupportedText() {
        assertThat(classifier.classify("오늘 기분이 좋아")).isEqualTo(Intent.UNKNOWN);
    }
}
