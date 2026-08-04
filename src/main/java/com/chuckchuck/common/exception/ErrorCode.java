package com.chuckchuck.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 내용을 확인해 주세요.", "말씀하신 내용을 다시 확인해 주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", "로그인한 뒤 다시 이용해 주세요."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 정보를 찾을 수 없습니다.", "요청하신 정보를 찾지 못했어요."),
    SESSION_EXPIRED(HttpStatus.CONFLICT, "세션이 만료되었습니다.", "죄송해요, 다시 한번 말씀해주시겠어요?"),
    INTENT_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "요청 의도를 파악하지 못했습니다.", "죄송해요. 하고 싶은 일을 다시 말씀해 주세요."),
    EXTERNAL_API_FAIL(HttpStatus.BAD_GATEWAY, "외부 서비스 연결에 실패했습니다.", "지금은 연결이 원활하지 않아요. 잠시 후 다시 해 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", "죄송해요. 잠시 후 다시 해 주세요.");

    private final HttpStatus status;
    private final String message;
    private final String ttsText;

    ErrorCode(HttpStatus status, String message, String ttsText) {
        this.status = status;
        this.message = message;
        this.ttsText = ttsText;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    public String ttsText() {
        return ttsText;
    }
}
