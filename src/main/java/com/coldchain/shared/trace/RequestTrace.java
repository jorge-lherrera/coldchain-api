package com.coldchain.shared.trace;

import com.coldchain.shared.identifier.UuidV7;
import org.slf4j.MDC;

public final class RequestTrace {

    public static final String HEADER = "X-Request-Id";

    public static final String MDC_KEY = "traceId";

    private static final int MAXIMUM_LENGTH = 64;

    private RequestTrace() {
    }

    public static String current() {
        String traceId = MDC.get(MDC_KEY);
        return traceId == null ? "untraced" : traceId;
    }

    static String adopt(String incoming) {
        String traceId = sanitise(incoming);
        MDC.put(MDC_KEY, traceId);
        return traceId;
    }

    static void clear() {
        MDC.remove(MDC_KEY);
    }

    private static String sanitise(String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return UuidV7.generate().toString();
        }
        String trimmed = incoming.trim();
        if (trimmed.length() > MAXIMUM_LENGTH || !trimmed.matches("[A-Za-z0-9._-]+")) {
            return UuidV7.generate().toString();
        }
        return trimmed;
    }
}
