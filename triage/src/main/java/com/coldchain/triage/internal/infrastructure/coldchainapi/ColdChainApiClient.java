package com.coldchain.triage.internal.infrastructure.coldchainapi;

import com.coldchain.triage.internal.domain.model.ExcursionContext;
import com.coldchain.triage.internal.domain.service.ExcursionContextSource;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class ColdChainApiClient implements ExcursionContextSource {

    private static final Logger LOG = LoggerFactory.getLogger(ColdChainApiClient.class);

    private static final String CONTEXT = "/internal/v1/shipments/{shipmentId}/excursions/{excursionId}/context";

    private static final ParameterizedTypeReference<ApiEnvelope<ExcursionContextPayload>> CONTEXT_ANSWER =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient api;

    private final PlatformTokenClient tokens;

    private final Retry retry;

    private final CircuitBreaker breaker;

    private final ColdChainApiProperties properties;

    public ColdChainApiClient(RestClient coldChainApi, PlatformTokenClient tokens, Retry coldChainApiRetry,
            CircuitBreaker coldChainApiCircuitBreaker, ColdChainApiProperties properties) {
        this.api = coldChainApi;
        this.tokens = tokens;
        this.retry = coldChainApiRetry;
        this.breaker = coldChainApiCircuitBreaker;
        this.properties = properties;
    }

    @Override
    public Optional<ExcursionContext> contextOf(UUID shipmentId, UUID excursionId) {
        if (!properties.configured()) {
            LOG.warn("No platform credential is configured: excursion {} is triaged without context", excursionId);
            return Optional.empty();
        }
        Supplier<ExcursionContext> guarded = CircuitBreaker.decorateSupplier(breaker,
                Retry.decorateSupplier(retry, () -> fetch(shipmentId, excursionId)));
        try {
            return Optional.of(guarded.get());
        } catch (HttpClientErrorException.NotFound unknown) {
            LOG.warn("The API knows no excursion {} under shipment {}: triaged without context", excursionId,
                    shipmentId);
            return Optional.empty();
        } catch (RuntimeException unavailable) {
            LOG.warn("The API gave no context for excursion {}, so it is triaged without it: {}", excursionId,
                    unavailable.toString());
            return Optional.empty();
        }
    }

    private ExcursionContext fetch(UUID shipmentId, UUID excursionId) {
        try {
            return ask(shipmentId, excursionId);
        } catch (HttpClientErrorException.Unauthorized expired) {
            tokens.forget();
            return ask(shipmentId, excursionId);
        }
    }

    private ExcursionContext ask(UUID shipmentId, UUID excursionId) {
        ApiEnvelope<ExcursionContextPayload> answer = api.get()
                .uri(CONTEXT, shipmentId, excursionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.current())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(CONTEXT_ANSWER);
        if (answer == null || answer.data() == null) {
            throw new IllegalStateException("The API answered the context of excursion " + excursionId
                    + " with nothing in it");
        }
        return answer.data().toContext();
    }
}
