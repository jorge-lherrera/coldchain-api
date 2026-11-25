package com.coldchain.triage.internal.infrastructure.coldchainapi;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * The call to the API is bounded three ways. The timeout lives in the HTTP client rather than in a
 * Resilience4j time limiter, because the client abandons the socket and a time limiter would only
 * abandon a thread. Only an API that did not answer, or answered 5xx, is retried and counted by the
 * breaker: a 4xx is an answer, and asking again would get the same one.
 */
@Configuration
@EnableConfigurationProperties(ColdChainApiProperties.class)
public class ColdChainApiConfig {

    private static final String NAME = "coldchain-api";

    private static final double BACKOFF_MULTIPLIER = 2.0;

    private static final int BREAKER_WINDOW = 10;

    private static final float FAILURE_RATE_PERCENT = 50;

    @Bean
    RestClient coldChainApi(RestClient.Builder builder, ColdChainApiProperties properties) {
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.timeout())
                .build());
        requests.setReadTimeout(properties.timeout());
        return builder.clone()
                .baseUrl(properties.baseUrl())
                .requestFactory(requests)
                .build();
    }

    @Bean
    Retry coldChainApiRetry(ColdChainApiProperties properties) {
        return Retry.of(NAME, RetryConfig.custom()
                .maxAttempts(properties.attempts())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(properties.backoff(), BACKOFF_MULTIPLIER))
                .retryExceptions(ResourceAccessException.class, HttpServerErrorException.class)
                .build());
    }

    @Bean
    CircuitBreaker coldChainApiCircuitBreaker(ColdChainApiProperties properties) {
        return CircuitBreaker.of(NAME, CircuitBreakerConfig.custom()
                .slidingWindowSize(BREAKER_WINDOW)
                .minimumNumberOfCalls(properties.minimumCalls())
                .failureRateThreshold(FAILURE_RATE_PERCENT)
                .waitDurationInOpenState(properties.openFor())
                .recordExceptions(ResourceAccessException.class, HttpServerErrorException.class)
                .build());
    }
}
