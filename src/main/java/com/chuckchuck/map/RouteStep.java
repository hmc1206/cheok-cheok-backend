package com.chuckchuck.map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RouteStep(
        String type,
        String desc,
        int durationMinutes,
        String boardingStop,
        String line,
        String boardingStation
) {
    public static RouteStep walk(String desc, int durationMinutes) {
        return new RouteStep("WALK", desc, durationMinutes, null, null, null);
    }

    public static RouteStep bus(String desc, int durationMinutes, String boardingStop) {
        return new RouteStep("BUS", desc, durationMinutes, boardingStop, null, null);
    }

    public static RouteStep subway(
            String desc,
            int durationMinutes,
            String line,
            String boardingStation
    ) {
        return new RouteStep("SUBWAY", desc, durationMinutes, null, line, boardingStation);
    }
}
