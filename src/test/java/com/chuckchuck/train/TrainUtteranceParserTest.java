package com.chuckchuck.train;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class TrainUtteranceParserTest {
    private final TrainUtteranceParser parser = new TrainUtteranceParser(
            Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneId.of("Asia/Seoul"))
    );

    @Test
    void extractsRouteFromNaturalSentence() {
        String text = "서울역에서 부산 가는 기차표 끊어줘";

        assertThat(parser.departure(text)).contains("서울");
        assertThat(parser.arrival(text)).contains("부산");
    }

    @Test
    void convertsRelativeDateAndKoreanTime() {
        assertThat(parser.date("내일 오후 2시")).contains("2026-08-02");
        assertThat(parser.time("내일 오후 2시")).contains("14:00");
    }

    @Test
    void rejectsInvalidTime() {
        assertThat(parser.time("오후 25시")).isEmpty();
    }
}
