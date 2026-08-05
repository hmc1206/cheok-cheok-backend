package com.chuckchuck.kiosk;

public record KioskScenarioSummary(
        String id,
        String title,
        String thumbnailUrl,
        String difficulty
) {
    public static KioskScenarioSummary from(KioskScenario scenario) {
        return new KioskScenarioSummary(
                scenario.id(),
                scenario.title(),
                scenario.thumbnailUrl(),
                scenario.difficulty()
        );
    }
}
