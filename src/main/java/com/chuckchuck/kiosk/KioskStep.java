package com.chuckchuck.kiosk;

import java.util.List;

public record KioskStep(
        String imageUrl,
        String guideText,
        String ttsText,
        List<TapTarget> tapTargets,
        String correctElementId,
        String retryTtsText,
        String retryHintImageUrl
) {
}
