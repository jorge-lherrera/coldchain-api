package com.coldchain.triage.internal.infrastructure.coldchainapi;

public final class ApiAnswers {

    private ApiAnswers() {
    }

    public static String token(String accessToken, long expiresInSeconds) {
        return """
                {"status":201,"code":"CLIENT_TOKEN_ISSUED","data":{"accessToken":"%s","refreshToken":null,\
                "tokenType":"Bearer","expiresInSeconds":%d},"traceId":"fake"}""".formatted(accessToken,
                expiresInSeconds);
    }

    public static String context(int maxSingleExcursionMinutes) {
        return """
                {"status":200,"code":"EXCURSION_CONTEXT_RETRIEVED","data":{\
                "shipmentId":"01930000-0000-7000-8000-00000000000a",\
                "excursion":{"id":"01930000-0000-7000-8000-00000000000c","kind":"ABOVE_MAX","status":"CLOSED"},\
                "windowFrom":"2026-05-01T09:35:00Z","windowTo":"2026-05-01T11:20:00Z",\
                "readings":[{"measuredAt":"2026-05-01T09:35:00Z","celsius":4.50},\
                {"measuredAt":"2026-05-01T10:05:00Z","celsius":11.50}],\
                "thresholds":{"minCelsius":2.00,"maxCelsius":8.00,"maxSingleExcursionMinutes":%d,\
                "maxCumulativeExcursionMinutes":120,"minCoveragePercent":80.00},\
                "custodian":"CARRIER","minutesSinceLastHandoff":25,\
                "handoffs":[{"occurredAt":"2026-05-01T09:40:00Z","from":"SHIPPER","to":"CARRIER"}]},\
                "traceId":"fake"}""".formatted(maxSingleExcursionMinutes);
    }
}
