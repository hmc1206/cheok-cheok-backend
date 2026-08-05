package com.chuckchuck.map;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class MapApiClient {

    public Optional<RouteResult> findRoute(String origin, String destination) {
        // 지도 공급자가 확정되기 전까지 화면과 TTS 흐름을 검증할 수 있는 대중교통 Mock 경로를 반환한다.
        return Optional.of(new RouteResult(
                35,
                1,
                1_400,
                List.of(
                        RouteStep.walk("정류장까지 걸어서 3분", 3),
                        RouteStep.bus("302번 버스를 타고 8정거장 이동", 25, "행복아파트"),
                        RouteStep.walk("도착지까지 걸어서 2분", 2)
                )
        ));
    }
}
