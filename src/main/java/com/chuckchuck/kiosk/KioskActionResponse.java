package com.chuckchuck.kiosk;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KioskActionResponse(
        String result,
        int stepIndex,
        int totalSteps,
        String imageUrl,
        String guideText,
        String ttsText,
        List<TapTarget> tapTargets,
        Boolean isComplete,
        String screen,
        String retryHintImageUrl
) {
    public static KioskActionResponse wrong(int stepIndex, int totalSteps, KioskStep step) {
        return new KioskActionResponse(
                "WRONG",
                stepIndex,
                totalSteps,
                null,
                null,
                step.retryTtsText(),
                null,
                null,
                null,
                step.retryHintImageUrl()
        );
    }

    public static KioskActionResponse next(int stepIndex, int totalSteps, KioskStep step) {
        return new KioskActionResponse(
                "CORRECT",
                stepIndex,
                totalSteps,
                step.imageUrl(),
                step.guideText(),
                "잘하셨어요! " + step.ttsText(),
                step.tapTargets(),
                null,
                null,
                null
        );
    }

    public static KioskActionResponse complete(int totalSteps) {
        return new KioskActionResponse(
                "CORRECT",
                totalSteps,
                totalSteps,
                null,
                null,
                "축하해요! 키오스크 연습을 모두 마치셨어요.",
                null,
                true,
                "KIOSK_COMPLETE",
                null
        );
    }
}
