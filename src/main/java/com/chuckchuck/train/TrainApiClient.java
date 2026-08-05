package com.chuckchuck.train;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class TrainApiClient {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public List<TrainCandidate> search(Map<String, Object> slots) {
        // 코레일 연동 규격이 확정되기 전까지 입력 시간 직후의 열차를 고정된 Mock 결과로 제공한다.
        LocalTime requestedTime = LocalTime.parse((String) slots.get("time"), TIME_FORMAT);
        LocalTime departureTime = requestedTime.plusMinutes(5);
        return List.of(new TrainCandidate(
                "KTX-101",
                departureTime.format(TIME_FORMAT),
                departureTime.plusHours(2).plusMinutes(40).format(TIME_FORMAT),
                59_800,
                true
        ));
    }
}
