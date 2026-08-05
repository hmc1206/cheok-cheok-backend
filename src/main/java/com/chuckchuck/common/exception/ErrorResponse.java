package com.chuckchuck.common.exception;

public record ErrorResponse(String errorCode, String message, String ttsText) {
}
