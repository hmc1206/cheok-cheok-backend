package com.chuckchuck.common.exception;

import org.springframework.http.HttpStatus;

/**
 * ttsText는 어르신이 귀로만 듣는 문장이므로 세 가지 규칙을 지킨다.
 * 1. [무슨 일이 있었는지] + [무엇을 하면 되는지] 두 문장 이내로 말한다.
 * 2. 사용자가 고칠 수 있는 것만 부탁한다. 서버나 앱 문제면 사과하고 기다려 달라고만 한다.
 * 3. 어미는 "~해 주세요"로 통일한다. 의문형은 선택지를 드릴 때만 쓴다.
 * message는 로그와 디버깅용이라 개발자 표현을 그대로 둔다.
 */
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 내용을 확인해 주세요.", "죄송해요. 다시 한번 말씀해 주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", "로그인이 필요해요. 로그인한 뒤 다시 말씀해 주세요."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 정보를 찾을 수 없습니다.", "요청하신 정보를 찾지 못했어요. 다시 한번 말씀해 주세요."),
    SESSION_EXPIRED(HttpStatus.CONFLICT, "세션이 만료되었습니다.", "죄송해요. 다시 한번 말씀해 주세요."),
    SESSION_INTERRUPTED(HttpStatus.CONFLICT, "진행 중인 대화가 중단되었습니다.", "하던 일을 멈추고 새로 시작할까요?"),
    INTENT_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "요청 의도를 파악하지 못했습니다.", "죄송해요. 하고 싶은 일을 다시 말씀해 주세요."),
    STT_EMPTY_INPUT(HttpStatus.UNPROCESSABLE_ENTITY, "음성에서 말소리를 찾지 못했습니다.", "잘 못 들었어요. 다시 한번 말씀해 주세요."),
    EXTERNAL_API_FAIL(HttpStatus.BAD_GATEWAY, "외부 서비스 연결에 실패했습니다.", "지금은 연결이 원활하지 않아요. 잠시 후 다시 해 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", "죄송해요. 잠시 후 다시 해 주세요."),
    APP_NOT_INSTALLED(HttpStatus.NOT_FOUND, "대상 앱이 설치되어 있지 않습니다.", "앱이 없어서 웹으로 열어드릴게요."),
    APP_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "앱 권한이 거부되었습니다.", "권한이 없어 열지 못했어요. 권한을 허용한 뒤 다시 말씀해 주세요."),
    UNSAFE_APP_BLOCKED(HttpStatus.FORBIDDEN, "허용되지 않은 앱 연결입니다.", "안전을 위해 열지 않았어요. 다른 걸 말씀해 주세요."),
    GEOCODE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다.", "장소를 찾지 못했어요. 다시 말씀해 주세요."),
    GEOCODE_API_FAIL(HttpStatus.BAD_GATEWAY, "지도 서비스 연결에 실패했습니다.", "지도 연결이 원활하지 않아요. 잠시 후 다시 해 주세요."),
    WEATHER_LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "날씨를 조회할 지역을 찾지 못했습니다.", "지역을 찾지 못했어요. 다시 말씀해 주세요."),
    WEATHER_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 날짜의 날씨 정보를 찾지 못했습니다.", "그 날짜의 날씨는 아직 알 수 없어요. 다른 날짜를 말씀해 주세요."),
    WEATHER_DATE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 예보 날짜입니다.", "너무 먼 날짜는 알 수 없어요. 가까운 날짜를 말씀해 주세요."),
    WEATHER_API_FAIL(HttpStatus.BAD_GATEWAY, "날씨 서비스 연결에 실패했습니다.", "지금은 날씨를 가져오지 못했어요. 잠시 후 다시 해 주세요."),
    MAX_RETRY_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "재질문 횟수를 초과했습니다.", "화면에서 직접 선택해 주시겠어요?");

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
