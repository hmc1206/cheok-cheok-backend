package com.chuckchuck.kiosk;

public record KioskSessionState(String sessionId, String scenarioId, int stepIndex) {
}
