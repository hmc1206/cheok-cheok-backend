package com.chuckchuck.kiosk;

public record TapTarget(
        String elementId,
        int x,
        int y,
        int width,
        int height
) {
}
