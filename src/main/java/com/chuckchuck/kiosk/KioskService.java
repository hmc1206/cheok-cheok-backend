package com.chuckchuck.kiosk;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

@Service
public class KioskService {
    private final KioskCatalog kioskCatalog;
    private final KioskSessionStore sessionStore;

    public KioskService(KioskCatalog kioskCatalog, KioskSessionStore sessionStore) {
        this.kioskCatalog = kioskCatalog;
        this.sessionStore = sessionStore;
    }

    public KioskScenarioListResponse findScenarios() {
        List<KioskScenarioSummary> scenarios = kioskCatalog.findAll().stream()
                .map(KioskScenarioSummary::from)
                .toList();
        return new KioskScenarioListResponse(scenarios);
    }

    public KioskStartResponse start(String scenarioId) {
        KioskScenario scenario = scenario(scenarioId);
        String sessionId = "ks_" + UUID.randomUUID();
        sessionStore.save(new KioskSessionState(sessionId, scenarioId, 0));

        KioskStep firstStep = scenario.steps().getFirst();
        return new KioskStartResponse(
                sessionId,
                1,
                scenario.steps().size(),
                firstStep.imageUrl(),
                firstStep.guideText(),
                firstStep.ttsText(),
                firstStep.tapTargets()
        );
    }

    public KioskActionResponse action(String scenarioId, KioskActionRequest request) {
        validate(request);
        KioskSessionState state = sessionStore.find(request.sessionId())
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_EXPIRED));
        if (!state.scenarioId().equals(scenarioId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        KioskScenario scenario = scenario(scenarioId);
        if (state.stepIndex() < 0 || state.stepIndex() >= scenario.steps().size()) {
            sessionStore.clear(state.sessionId());
            throw new ApiException(ErrorCode.SESSION_EXPIRED);
        }

        KioskStep currentStep = scenario.steps().get(state.stepIndex());
        if (!currentStep.correctElementId().equals(request.tappedElementId())) {
            sessionStore.save(state);
            return KioskActionResponse.wrong(
                    state.stepIndex() + 1,
                    scenario.steps().size(),
                    currentStep
            );
        }

        int nextIndex = state.stepIndex() + 1;
        if (nextIndex == scenario.steps().size()) {
            sessionStore.clear(state.sessionId());
            return KioskActionResponse.complete(scenario.steps().size());
        }

        // 내부 배열은 0부터 세지만 화면의 단계 번호는 어르신이 이해하기 쉽게 1부터 반환한다.
        sessionStore.save(new KioskSessionState(state.sessionId(), scenarioId, nextIndex));
        return KioskActionResponse.next(
                nextIndex + 1,
                scenario.steps().size(),
                scenario.steps().get(nextIndex)
        );
    }

    private KioskScenario scenario(String scenarioId) {
        return kioskCatalog.findById(scenarioId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "키오스크 시나리오를 찾을 수 없습니다.",
                        "선택한 연습을 찾지 못했어요. 다른 연습을 선택해 주세요."
                ));
    }

    private void validate(KioskActionRequest request) {
        if (request == null
                || request.sessionId() == null || request.sessionId().isBlank()
                || request.tappedElementId() == null || request.tappedElementId().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }
}
