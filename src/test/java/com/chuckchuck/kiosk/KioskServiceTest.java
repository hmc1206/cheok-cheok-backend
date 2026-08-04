package com.chuckchuck.kiosk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KioskServiceTest {
    private KioskSessionStore sessionStore;
    private KioskService service;

    @BeforeEach
    void setUp() {
        sessionStore = mock(KioskSessionStore.class);
        service = new KioskService(new KioskCatalog(), sessionStore);
    }

    @Test
    void startsScenarioAtFirstStep() {
        KioskStartResponse response = service.start("kiosk_cafe");

        assertThat(response.stepIndex()).isEqualTo(1);
        assertThat(response.totalSteps()).isEqualTo(5);
        assertThat(response.tapTargets()).singleElement()
                .extracting(TapTarget::elementId)
                .isEqualTo("btn_order");
        verify(sessionStore).save(argThat(state ->
                state.sessionId().equals(response.sessionId()) && state.stepIndex() == 0));
    }

    @Test
    void keepsStepAndRefreshesSessionAfterWrongTap() {
        KioskSessionState state = new KioskSessionState("ks_001", "kiosk_cafe", 0);
        when(sessionStore.find("ks_001")).thenReturn(Optional.of(state));

        KioskActionResponse response = service.action(
                "kiosk_cafe",
                new KioskActionRequest("ks_001", "wrong_button")
        );

        assertThat(response.result()).isEqualTo("WRONG");
        assertThat(response.stepIndex()).isEqualTo(1);
        verify(sessionStore).save(state);
    }

    @Test
    void advancesAfterCorrectTap() {
        when(sessionStore.find("ks_001"))
                .thenReturn(Optional.of(new KioskSessionState("ks_001", "kiosk_cafe", 0)));

        KioskActionResponse response = service.action(
                "kiosk_cafe",
                new KioskActionRequest("ks_001", "btn_order")
        );

        assertThat(response.result()).isEqualTo("CORRECT");
        assertThat(response.stepIndex()).isEqualTo(2);
        assertThat(response.tapTargets()).singleElement()
                .extracting(TapTarget::elementId)
                .isEqualTo("menu_americano");
        verify(sessionStore).save(argThat(state -> state.stepIndex() == 1));
    }

    @Test
    void clearsSessionAfterLastCorrectTap() {
        when(sessionStore.find("ks_001"))
                .thenReturn(Optional.of(new KioskSessionState("ks_001", "kiosk_cafe", 4)));

        KioskActionResponse response = service.action(
                "kiosk_cafe",
                new KioskActionRequest("ks_001", "btn_pay")
        );

        assertThat(response.isComplete()).isTrue();
        assertThat(response.screen()).isEqualTo("KIOSK_COMPLETE");
        verify(sessionStore).clear("ks_001");
    }
}
