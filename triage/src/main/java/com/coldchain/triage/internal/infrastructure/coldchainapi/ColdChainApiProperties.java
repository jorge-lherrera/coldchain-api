package com.coldchain.triage.internal.infrastructure.coldchainapi;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "coldchain.triage.api")
public record ColdChainApiProperties(
        @DefaultValue("http://localhost:8080/api") String baseUrl,
        @DefaultValue("coldchain-triage") String clientId,
        String clientSecret,
        @DefaultValue("2s") Duration timeout,
        @DefaultValue("3") int attempts,
        @DefaultValue("500ms") Duration backoff,
        @DefaultValue("5") int minimumCalls,
        @DefaultValue("30s") Duration openFor) {

    public boolean configured() {
        return clientSecret != null && !clientSecret.isBlank();
    }
}
