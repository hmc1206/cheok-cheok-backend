package com.chuckchuck.voice;

/** 화면에는 label을 크게 표시하고, 선택하면 value를 다음 요청의 text로 보낸다. */
public record QuickReply(String label, String value) {
}
