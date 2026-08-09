package com.chuckchuck.voice;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드가 음성 또는 버튼 입력을 보내는 대화 API의 HTTP 진입점이다.
 * 클래스 경로 {@code /voice}와 메서드 경로 {@code /process}가 합쳐져
 * 최종 요청 주소는 {@code POST /voice/process}가 된다.
 */
@RestController
@RequestMapping("/voice")
public class VoiceController {
    private final VoiceService voiceService;

    public VoiceController(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    /**
     * JSON 요청을 VoiceRequest로 변환해 서비스에 전달한다.
     * 입력 검증, Redis 세션 조회, 의도 분류와 응답 생성은 VoiceService가 담당한다.
     */
    @PostMapping("/process")
    public VoiceResponse process(@RequestBody VoiceRequest request) {
        return voiceService.process(request);
    }
}
