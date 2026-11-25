package com.coldchain.triage.internal.infrastructure.coldchainapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.coldchain.triage.internal.domain.model.CustodyParticipation;
import com.coldchain.triage.internal.domain.model.ExcursionContext;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ColdChainApiClientTest {

    private static final String BASE = "http://api.test/api";

    private static final String TOKEN_URL = BASE + "/v1/auth/client-token";

    private static final UUID SHIPMENT = UUID.fromString("01930000-0000-7000-8000-00000000000a");

    private static final UUID EXCURSION = UUID.fromString("01930000-0000-7000-8000-00000000000c");

    private static final String CONTEXT_URL =
            BASE + "/internal/v1/shipments/" + SHIPMENT + "/excursions/" + EXCURSION + "/context";

    private static final long FIFTEEN_MINUTES = 900;

    private final MovableClock clock = new MovableClock(Instant.parse("2026-05-01T11:00:00Z"));

    @Test
    void theContextComesBackAskedForWithThePlatformToken() {
        Fixture fixture = new Fixture(properties("platform-secret", 3, 5));
        fixture.server.expect(once(), requestTo(TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(ApiAnswers.token("first", FIFTEEN_MINUTES)));
        fixture.server.expect(once(), requestTo(CONTEXT_URL)).andExpect(header("Authorization", "Bearer first"))
                .andRespond(withSuccess(ApiAnswers.context(60), MediaType.APPLICATION_JSON));

        ExcursionContext context = fixture.client.contextOf(SHIPMENT, EXCURSION).orElseThrow();

        assertThat(context.custodian()).isEqualTo(CustodyParticipation.CARRIER);
        assertThat(context.thresholds().maxSingleExcursionMinutes()).isEqualTo(60);
        assertThat(context.readings()).hasSize(2);
        assertThat(context.minutesSinceLastHandoff()).isEqualTo(25);
        assertThat(context.handoffs()).singleElement()
                .satisfies(handoff -> assertThat(handoff.to()).isEqualTo(CustodyParticipation.CARRIER));
        fixture.server.verify();
    }

    @Test
    void theTokenIsKeptUntilShortlyBeforeItExpires() {
        Fixture fixture = new Fixture(properties("platform-secret", 3, 5));
        fixture.server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(ApiAnswers.token("first", FIFTEEN_MINUTES)));
        fixture.server.expect(times(2), requestTo(CONTEXT_URL)).andExpect(header("Authorization", "Bearer first"))
                .andRespond(withSuccess(ApiAnswers.context(60), MediaType.APPLICATION_JSON));
        fixture.server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(ApiAnswers.token("second", FIFTEEN_MINUTES)));
        fixture.server.expect(once(), requestTo(CONTEXT_URL)).andExpect(header("Authorization", "Bearer second"))
                .andRespond(withSuccess(ApiAnswers.context(60), MediaType.APPLICATION_JSON));

        fixture.client.contextOf(SHIPMENT, EXCURSION);
        clock.advance(Duration.ofMinutes(10));
        fixture.client.contextOf(SHIPMENT, EXCURSION);
        clock.advance(Duration.ofMinutes(4).plusSeconds(1));
        fixture.client.contextOf(SHIPMENT, EXCURSION);

        fixture.server.verify();
    }

    @Test
    void aRejectedTokenIsReplacedRatherThanRetried() {
        Fixture fixture = new Fixture(properties("platform-secret", 3, 5));
        fixture.server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(ApiAnswers.token("revoked", FIFTEEN_MINUTES)));
        fixture.server.expect(once(), requestTo(CONTEXT_URL)).andExpect(header("Authorization", "Bearer revoked"))
                .andRespond(withUnauthorizedRequest());
        fixture.server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(ApiAnswers.token("fresh", FIFTEEN_MINUTES)));
        fixture.server.expect(once(), requestTo(CONTEXT_URL)).andExpect(header("Authorization", "Bearer fresh"))
                .andRespond(withSuccess(ApiAnswers.context(60), MediaType.APPLICATION_JSON));

        assertThat(fixture.client.contextOf(SHIPMENT, EXCURSION)).isPresent();
        fixture.server.verify();
    }

    @Test
    void aServerErrorIsRetriedAndThenTheTriageGoesOnWithoutContext() {
        Fixture fixture = new Fixture(properties("platform-secret", 3, 5));
        fixture.server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(ApiAnswers.token("first", FIFTEEN_MINUTES)));
        fixture.server.expect(times(3), requestTo(CONTEXT_URL)).andRespond(withServiceUnavailable());

        assertThat(fixture.client.contextOf(SHIPMENT, EXCURSION))
                .describedAs("no context is not a failure: the alert goes out without it")
                .isEmpty();
        fixture.server.verify();
    }

    @Test
    void anApiThatDoesNotAnswerIsRetriedLikeAServerError() {
        Fixture fixture = new Fixture(properties("platform-secret", 2, 5));
        fixture.server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(ApiAnswers.token("first", FIFTEEN_MINUTES)));
        fixture.server.expect(times(2), requestTo(CONTEXT_URL))
                .andRespond(withException(new IOException("Connection refused")));

        assertThat(fixture.client.contextOf(SHIPMENT, EXCURSION)).isEmpty();
        fixture.server.verify();
    }

    @Test
    void anExcursionTheApiDoesNotKnowIsAnAnswerAndIsNotAskedAgain() {
        Fixture fixture = new Fixture(properties("platform-secret", 3, 5));
        fixture.server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(ApiAnswers.token("first", FIFTEEN_MINUTES)));
        fixture.server.expect(once(), requestTo(CONTEXT_URL)).andRespond(withResourceNotFound());

        assertThat(fixture.client.contextOf(SHIPMENT, EXCURSION)).isEmpty();
        fixture.server.verify();
    }

    @Test
    void theBreakerStopsAskingWhileTheApiIsDown() {
        Fixture fixture = new Fixture(properties("platform-secret", 1, 2));
        fixture.server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(ApiAnswers.token("first", FIFTEEN_MINUTES)));
        fixture.server.expect(times(2), requestTo(CONTEXT_URL)).andRespond(withServiceUnavailable());

        fixture.client.contextOf(SHIPMENT, EXCURSION);
        fixture.client.contextOf(SHIPMENT, EXCURSION);
        Optional<ExcursionContext> whileOpen = fixture.client.contextOf(SHIPMENT, EXCURSION);

        assertThat(whileOpen).isEmpty();
        fixture.server.verify();
    }

    @Test
    void withoutACredentialTheApiIsNeverAsked() {
        Fixture fixture = new Fixture(properties("", 3, 5));
        fixture.server.expect(never(), requestTo(TOKEN_URL));

        assertThat(fixture.client.contextOf(SHIPMENT, EXCURSION)).isEmpty();
        fixture.server.verify();
    }

    private static ColdChainApiProperties properties(String secret, int attempts, int minimumCalls) {
        return new ColdChainApiProperties(BASE, "coldchain-triage", secret, Duration.ofSeconds(2), attempts,
                Duration.ofMillis(1), minimumCalls, Duration.ofSeconds(30));
    }

    private final class Fixture {

        private final MockRestServiceServer server;

        private final ColdChainApiClient client;

        private Fixture(ColdChainApiProperties properties) {
            RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
            server = MockRestServiceServer.bindTo(builder).build();
            RestClient api = builder.build();
            ColdChainApiConfig config = new ColdChainApiConfig();
            client = new ColdChainApiClient(api, new PlatformTokenClient(api, properties, clock),
                    config.coldChainApiRetry(properties), config.coldChainApiCircuitBreaker(properties),
                    properties);
        }
    }

    private static final class MovableClock extends Clock {

        private Instant now;

        private MovableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration step) {
            now = now.plus(step);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
