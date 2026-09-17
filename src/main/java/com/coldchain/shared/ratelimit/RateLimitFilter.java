package com.coldchain.shared.ratelimit;

import com.coldchain.shared.error.CoreErrorCode;
import com.coldchain.shared.error.ProblemErrorResponder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final long MAXIMUM_TRACKED_CLIENTS = 100_000L;

    private final RequestMatcher limited;

    private final RateLimitProperties properties;

    private final ClientAddress clientAddress;

    private final ProblemErrorResponder responder;

    private final Cache<String, Bucket> buckets;

    public RateLimitFilter(RequestMatcher limited, RateLimitProperties properties,
            ClientAddress clientAddress, ProblemErrorResponder responder) {
        this.limited = limited;
        this.properties = properties;
        this.clientAddress = clientAddress;
        this.responder = responder;
        this.buckets = Caffeine.newBuilder()
                .maximumSize(MAXIMUM_TRACKED_CLIENTS)
                .expireAfterAccess(properties.window().multipliedBy(2))
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || !limited.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        Bucket bucket = buckets.get(keyOf(request), ignored -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }
        long retryAfter = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds() + 1;
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        responder.respond(request, response, CoreErrorCode.TOO_MANY_REQUESTS,
                "The client exceeded the allowed number of attempts on this endpoint",
                Map.of("retryAfter", retryAfter));
    }

    private String keyOf(HttpServletRequest request) {
        return clientAddress.of(request) + " " + request.getRequestURI();
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.capacity())
                        .refillGreedy(properties.capacity(), properties.window())
                        .build())
                .build();
    }
}
