package com.chuckchuck.common.exception;

/** v2.0 공통 오류 봉투. 프론트는 error.code로 분기하고 ttsText를 즉시 읽는다. */
public record ErrorResponse(boolean success, ErrorDetail error, String ttsText) {
    public static ErrorResponse from(ErrorCode errorCode, String message, String ttsText) {
        return new ErrorResponse(false, new ErrorDetail(errorCode.name(), message), ttsText);
    }

    public record ErrorDetail(String code, String message) {
    }
}
