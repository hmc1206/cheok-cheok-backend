package com.chuckchuck.hospital;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {
    private final MapService mapService;

    @PostMapping("/hospital")
    public ResponseEntity<MedicalRouteResponseDto> getMedicalRoute(@RequestBody MedicalRouteRequestDto request){
        MedicalRouteResponseDto response = mapService.processMedicalRoute(request);
        return ResponseEntity.ok(response);
    }
}
