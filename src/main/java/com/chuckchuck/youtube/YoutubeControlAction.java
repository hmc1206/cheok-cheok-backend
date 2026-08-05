package com.chuckchuck.youtube;

public enum YoutubeControlAction {
    PAUSE("일시정지했어요."),
    RESUME("다시 재생할게요."),
    NEXT("다음 영상을 재생할게요."),
    VOLUME_UP("소리를 키웠어요."),
    VOLUME_DOWN("소리를 줄였어요.");

    private final String ttsText;

    YoutubeControlAction(String ttsText) {
        this.ttsText = ttsText;
    }

    public String ttsText() {
        return ttsText;
    }
}
