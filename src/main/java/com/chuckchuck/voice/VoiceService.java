package com.chuckchuck.voice;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.chuckchuck.session.SessionService;
import com.chuckchuck.session.SessionState;

@Service
public class VoiceService {
    private static final String DONE = "DONE";

    private final SessionService sessionService;
    private final IntentClassifier intentClassifier;
    private final IntentRouter intentRouter;
    private final SpeechTranscriber speechTranscriber;

    public VoiceService(
            SessionService sessionService,
            IntentClassifier intentClassifier,
            IntentRouter intentRouter,
            SpeechTranscriber speechTranscriber
    ) {
        this.sessionService = sessionService;
        this.intentClassifier = intentClassifier;
        this.intentRouter = intentRouter;
        this.speechTranscriber = speechTranscriber;
    }

    public VoiceResponse process(VoiceRequest request) {
        validate(request);
        String userText = resolveText(request);

        SessionState session = sessionService.find(request.userId())
                .orElseGet(() -> new SessionState(
                        request.userId(),
                        intentClassifier.classify(userText),
                        "NEW",
                        null
                ));
        session = withWeatherCoordinates(session, request);

        VoiceResponse response = intentRouter.route(session.intent()).handle(session, userText);
        if (response.intent() != session.intent()) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }

        // 완료된 대화가 다음 발화에 영향을 주지 않도록 즉시 지우고, 진행 중인 대화만 TTL을 갱신한다.
        if (DONE.equals(response.step())) {
            sessionService.clear(request.userId());
        } else {
            sessionService.save(SessionState.from(request.userId(), response));
        }
        return response;
    }

    private void validate(VoiceRequest request) {
        if (request == null || request.userId() == null || request.userId().isBlank()) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "userId는 반드시 입력해야 합니다.",
                    "로그인 정보를 확인한 뒤 다시 말씀해 주세요."
            );
        }
        if (!request.hasText() && !request.hasAudio()) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "text와 audio 중 하나는 반드시 입력해야 합니다.",
                    "말씀하실 내용을 다시 입력해 주세요."
            );
        }
        if ((request.latitude() == null) != (request.longitude() == null)
                || request.latitude() != null && (request.latitude() < -90 || request.latitude() > 90)
                || request.longitude() != null && (request.longitude() < -180 || request.longitude() > 180)) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "위도와 경도를 올바른 범위로 함께 입력해야 합니다.",
                    "현재 위치 정보를 다시 확인해 주세요."
            );
        }
    }

    private String resolveText(VoiceRequest request) {
        // 음성 원본이 있으면 프론트의 임의 STT 결과 대신 서버 STT를 기준으로 대화를 진행한다.
        if (request.hasAudio()) {
            return speechTranscriber.transcribe(request.audio());
        }
        return request.text().trim();
    }

    // 기존 IntentHandler는 요청 객체를 받지 않으므로 날씨 대화에 한해 좌표를 세션 슬롯으로 전달한다.
    private SessionState withWeatherCoordinates(SessionState session, VoiceRequest request) {
        if (session.intent() != Intent.WEATHER_INFO || !request.hasCoordinates()) {
            return session;
        }
        Map<String, Object> slots = new LinkedHashMap<>(session.slots());
        slots.put("latitude", request.latitude());
        slots.put("longitude", request.longitude());
        return new SessionState(session.userId(), session.intent(), session.step(), slots);
    }
}
