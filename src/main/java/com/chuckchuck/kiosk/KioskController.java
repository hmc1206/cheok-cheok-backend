package com.chuckchuck.kiosk;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kiosk/scenarios")
public class KioskController {
    private final KioskService kioskService;

    public KioskController(KioskService kioskService) {
        this.kioskService = kioskService;
    }

    @GetMapping
    public KioskScenarioListResponse findScenarios() {
        return kioskService.findScenarios();
    }

    @PostMapping("/{scenarioId}/start")
    public KioskStartResponse start(@PathVariable String scenarioId) {
        return kioskService.start(scenarioId);
    }

    @PostMapping("/{scenarioId}/action")
    public KioskActionResponse action(
            @PathVariable String scenarioId,
            @RequestBody KioskActionRequest request
    ) {
        return kioskService.action(scenarioId, request);
    }
}
