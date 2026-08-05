package com.chuckchuck.youtube;

import java.util.Locale;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

@RestController
@RequestMapping("/api/youtube")
public class YoutubeController {

    @PostMapping("/control")
    public YoutubeControlResponse control(@RequestBody YoutubeControlRequest request) {
        if (request.userId() == null || request.userId().isBlank()
                || request.action() == null || request.action().isBlank()) {
            throw invalidAction();
        }

        try {
            YoutubeControlAction action = YoutubeControlAction.valueOf(request.action().toUpperCase(Locale.ROOT));
            // 실제 재생 조작은 브라우저 플레이어가 수행하고, 서버는 허용된 명령과 안내 문장을 확정한다.
            return new YoutubeControlResponse("SUCCESS", action.ttsText());
        } catch (IllegalArgumentException exception) {
            throw invalidAction();
        }
    }

    private ApiException invalidAction() {
        return new ApiException(
                ErrorCode.INVALID_REQUEST,
                "지원하지 않는 유튜브 제어 명령입니다.",
                "재생, 일시정지, 다음 영상, 소리 조절 중 하나를 말씀해 주세요."
        );
    }
}
