package com.chuckchuck.voice;

import com.chuckchuck.session.SessionState;

public interface IntentHandler {
    Intent supports();

    VoiceResponse handle(SessionState session, String userText);
}
