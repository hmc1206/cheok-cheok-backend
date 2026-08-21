package com.chuckchuck.voice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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

        Intent classifiedIntent = intentClassifier.classify(userText);
        SessionState session = selectSession(request.userId(), classifiedIntent);
        session = withCoordinates(session, request);

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

    private SessionState selectSession(String userId, Intent classifiedIntent) {
        Optional<SessionState> existingSession = sessionService.find(userId);

        // "네", "서울" 같은 후속 답변은 UNKNOWN으로 분류되므로 기존 멀티턴 대화를 이어간다.
        // 반대로 새 기능이 명확하면 남아 있던 대화를 버리고 NEW 단계에서 다시 시작한다.
        return existingSession
                .filter(session -> classifiedIntent == Intent.UNKNOWN || session.intent() == classifiedIntent)
                .orElseGet(() -> new SessionState(userId, classifiedIntent, "NEW", null));
    }

    private void validate(VoiceRequest request) {
        if (request == null || request.userId() == null || request.userId().isBlank()) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "userId는 반드시 입력해야 합니다.",
                    "로그인이 필요해요. 로그인한 뒤 다시 말씀해 주세요."
            );
        }
        if (!request.hasText() && !request.hasAudio()) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "text와 audio 중 하나는 반드시 입력해야 합니다.",
                    "잘 못 들었어요. 다시 한번 말씀해 주세요."
            );
        }
        if ((request.latitude() == null) != (request.longitude() == null)
                || request.latitude() != null && (request.latitude() < -90 || request.latitude() > 90)
                || request.longitude() != null && (request.longitude() < -180 || request.longitude() > 180)) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "위도와 경도를 올바른 범위로 함께 입력해야 합니다.",
                    "위치를 확인하지 못했어요. 잠시 후 다시 해 주세요."
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

    // 기존 IntentHandler 계약을 유지하면서 현재 위치 기반 기능에만 좌표를 전달한다.
    private SessionState withCoordinates(SessionState session, VoiceRequest request) {
        boolean supportsCoordinates = session.intent() == Intent.WEATHER_INFO
                || session.intent() == Intent.MEDICAL_ROUTE;
        if (!supportsCoordinates || !request.hasCoordinates()) {
            return session;
        }
        Map<String, Object> slots = new LinkedHashMap<>(session.slots());
        slots.put("latitude", request.latitude());
        slots.put("longitude", request.longitude());
        return new SessionState(session.userId(), session.intent(), session.step(), slots);
    }
}
