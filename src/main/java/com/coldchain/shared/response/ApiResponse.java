package com.coldchain.shared.response;

import java.time.Instant;

public record ApiResponse<T>(
        int status,
        String code,
        String messageKey,
        String message,
        T data,
        String traceId,
        Instant timestamp,
        ResponseMeta meta) {
}
