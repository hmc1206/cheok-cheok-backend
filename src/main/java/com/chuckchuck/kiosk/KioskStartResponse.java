package com.chuckchuck.kiosk;

import java.util.List;

public record KioskStartResponse(
        String sessionId,
        int stepIndex,
        int totalSteps,
        String imageUrl,
        String guideText,
        String ttsText,
        List<TapTarget> tapTargets
) {
}
