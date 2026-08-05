package com.chuckchuck.voice;

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

    public VoiceService(
            SessionService sessionService,
            IntentClassifier intentClassifier,
            IntentRouter intentRouter
    ) {
        this.sessionService = sessionService;
        this.intentClassifier = intentClassifier;
        this.intentRouter = intentRouter;
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
                    "text와 audioBase64 중 하나는 반드시 입력해야 합니다.",
                    "말씀하실 내용을 다시 입력해 주세요."
            );
        }
    }

    private String resolveText(VoiceRequest request) {
        if (request.hasText()) {
            return request.text().trim();
        }
        throw new ApiException(
                ErrorCode.EXTERNAL_API_FAIL,
                "음성을 텍스트로 변환하지 못했습니다.",
                "지금은 음성을 글자로 바꾸지 못했어요. 잠시 후 다시 말씀해 주세요."
        );
    }
}
