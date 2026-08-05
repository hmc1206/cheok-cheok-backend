package com.chuckchuck.common.exception;

public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String ttsText;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.message(), errorCode.ttsText());
    }

    public ApiException(ErrorCode errorCode, String message, String ttsText) {
        super(message);
        this.errorCode = errorCode;
        this.ttsText = ttsText;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public String ttsText() {
        return ttsText;
    }
}
