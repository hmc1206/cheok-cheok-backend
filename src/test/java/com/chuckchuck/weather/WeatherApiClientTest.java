package com.chuckchuck.weather;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class WeatherApiClientTest {

    @Test
    void convertsSeoulCoordinatesAndReadsLowerLeftGridOrder() {
        WeatherApiClient.GridPoint point = WeatherApiClient.toGridPoint(37.5665, 126.9780);
        String[] values = new String[149 * 253];
        Arrays.fill(values, "-99.00");
        values[point.index()] = "25.00";

        double[] grid = WeatherApiClient.parseGrid(String.join(",", values));

        assertThat(point).isEqualTo(new WeatherApiClient.GridPoint(60, 127));
        assertThat(grid[point.index()]).isEqualTo(25.0);
        assertThat(grid[0]).isNaN();
    }

    @Test
    void usesLastPublishedForecastTimeWithTwentyMinuteBuffer() {
        ZonedDateTime now = ZonedDateTime.of(
                2026, 8, 16, 23, 10, 0, 0,
                ZoneId.of("Asia/Seoul")
        );

        assertThat(WeatherApiClient.latestBaseTime(now))
                .isEqualTo(LocalDateTime.of(2026, 8, 16, 20, 0));
    }
}
