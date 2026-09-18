package com.coldchain.shared.ratelimit;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coldchain.rate-limit")
public record RateLimitProperties(boolean enabled, int capacity, Duration window,
        List<String> trustedProxies) {

    public RateLimitProperties {
        if (capacity <= 0) {
            throw new IllegalStateException("coldchain.rate-limit.capacity must be a positive number");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalStateException("coldchain.rate-limit.window must be a positive duration");
        }
        trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
    }
}
