package com.chuckchuck.map;

import java.util.List;

public record RouteResult(
        int durationMinutes,
        int transferCount,
        int totalFare,
        List<RouteStep> steps
) {
}
