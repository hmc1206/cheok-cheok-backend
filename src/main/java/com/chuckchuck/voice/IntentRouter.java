package com.chuckchuck.voice;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

@Component
public class IntentRouter {
    private final Map<Intent, IntentHandler> handlers = new EnumMap<>(Intent.class);

    public IntentRouter(List<IntentHandler> handlers) {
        for (IntentHandler handler : handlers) {
            IntentHandler previous = this.handlers.put(handler.supports(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate handler for intent: " + handler.supports());
            }
        }
    }

    public IntentHandler route(Intent intent) {
        IntentHandler handler = handlers.get(intent);
        if (handler == null) {
            throw new ApiException(ErrorCode.INTENT_NOT_FOUND);
        }
        return handler;
    }
}
