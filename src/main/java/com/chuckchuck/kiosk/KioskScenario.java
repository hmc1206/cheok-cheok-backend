package com.chuckchuck.kiosk;

import java.util.List;

public record KioskScenario(
        String id,
        String title,
        String thumbnailUrl,
        String difficulty,
        List<KioskStep> steps
) {
}
