package com.coldchain.modules.shipment.internal.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coldchain.handoff")
public record HandoffProperties(Duration codeLifetime) {

    public HandoffProperties {
        if (codeLifetime == null || codeLifetime.isZero() || codeLifetime.isNegative()) {
            throw new IllegalStateException("coldchain.handoff.code-lifetime must be a positive duration");
        }
    }
}
