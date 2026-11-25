package com.coldchain.triage.internal.infrastructure.coldchainapi;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PlatformTokenClient {

    private static final String CLIENT_TOKEN = "/v1/auth/client-token";

    private static final Duration RENEW_BEFORE_EXPIRY = Duration.ofSeconds(60);

    private static final ParameterizedTypeReference<ApiEnvelope<TokenPayload>> TOKEN =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient api;

    private final ColdChainApiProperties properties;

    private final Clock clock;

    private IssuedToken issued;

    public PlatformTokenClient(RestClient coldChainApi, ColdChainApiProperties properties, Clock clock) {
        this.api = coldChainApi;
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized String current() {
        Instant now = clock.instant();
        if (issued == null || !now.isBefore(issued.renewAt())) {
            issued = request(now);
        }
        return issued.value();
    }

    public synchronized void forget() {
        issued = null;
    }

    private IssuedToken request(Instant now) {
        ApiEnvelope<TokenPayload> answer = api.post()
                .uri(CLIENT_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new ClientTokenPayload(properties.clientId(), properties.clientSecret()))
                .retrieve()
                .body(TOKEN);
        if (answer == null || answer.data() == null || answer.data().accessToken() == null) {
            throw new IllegalStateException("The API issued a platform token with no token in it");
        }
        return new IssuedToken(answer.data().accessToken(),
                now.plusSeconds(answer.data().expiresInSeconds()).minus(RENEW_BEFORE_EXPIRY));
    }

    private record IssuedToken(String value, Instant renewAt) {
    }
}
