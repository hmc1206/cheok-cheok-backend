package com.chuckchuck.weather;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 음성 의도 분류를 거치지 않고 날씨 조회만 확인하는 QA용 API다.
 * 실제 앱의 음성 흐름은 POST /voice/process를 사용한다.
 */
@RestController
@RequestMapping("/api/v1/weather")
public class WeatherController {
    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public WeatherLookupResponse lookup(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String date
    ) {
        LocalDate forecastDate = weatherService.parseDate(date);
        return new WeatherLookupResponse(
                true,
                weatherService.lookup(location, latitude, longitude, forecastDate)
        );
    }

    public record WeatherLookupResponse(boolean success, WeatherData data) {
    }
}
