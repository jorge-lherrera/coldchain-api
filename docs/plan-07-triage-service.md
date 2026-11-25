# Build plan · 07 · triage service

> **Status:** in progress — not a module of the monolith: a service of its own.

The second service. It reads the excursion feed, asks the API what was happening around each
excursion, asks a language model for a probable cause and a summary — or the rules, when the model
does not answer — and publishes the alert back for the API to show
([ADR-010](adr/ADR-010-second-service-for-excursion-triage.md),
[ADR-012](adr/ADR-012-triage-is-advice-not-evidence.md)). The milestone ends when an excursion in the
walkthrough becomes an alert on the other side, with the model and without it.

It lives in `triage/`, an independent Gradle build with its own `settings.gradle` and its own wrapper:
`cd triage && ./gradlew check`. Two of its steps happen **in the API**, because triage needs them
there: the platform credential and the excursion context.

## The shape

The same shape as one API module ([ADR-007](adr/ADR-007-hexagonal-module-internals.md)), without a
`modules/` level, because the service is a single context:

```
com.coldchain.triage/
  TriageApplication.java
  delivery/messaging/        ExcursionFeedListener · ExcursionFeedMessage · ExcursionFeedMessageMapper
                             ExcursionFeedErrorHandlingConfig
  api/                       TriageApi · dto/ · event/AlertTriaged
  internal/
    application/             TriageFacade · usecase/command/{RecordExcursionEventUseCase, TriageExcursionUseCase}
    domain/
      model/                 ExcursionAlert · Triage · InboxEvent · ExcursionContext · ProbableCause · Severity
      repository/            ExcursionAlertRepository · InboxEventRepository
      service/               RuleBasedTriage · ExcursionContextSource · TriageAdvisor
    infrastructure/
      persistence/           entity · jpa · adapter · mapper
      coldchainapi/          ColdChainApiClient
      llm/                   SpringAiTriageAdvisor
      messaging/             AlertFeedConfig
    exception/               TriageErrorCode
  shared/                    error model, UUID v7
```

`ExcursionContextSource` and `TriageAdvisor` are outbound ports declared by the domain, the way
`identity` declares `SecretHasher`. Their adapters are the only classes that know HTTP or a model
exists.

## Step 1 · Foundations

- `triage/build.gradle`: Spring Boot 4.1, Java 25, Spring Kafka, Spring Data JPA, Flyway on
  PostgreSQL, Spring Modulith's event publication registry, Testcontainers, ArchUnit.
- **Migration V1, three tables**, PostgreSQL types:
  - `inbox_event` — `event_id uuid` primary key, `type`, `excursion_id`, `occurred_at`,
    `received_at`, `status` with `CHECK (status IN ('RECEIVED', 'PROCESSED'))`, `processed_at`.
  - `excursion_alert` — UUID v7 primary key and `excursion_id` unique. It holds:
    - the facts of the excursion: kind, opened, closed, peak, duration;
    - the triage: cause, severity, confidence, the three summaries, recommended action;
    - its provenance: source, model, prompt version, input and output tokens, latency,
      `context_available`, `triaged_at`;
    - a version column for optimistic locking.
  - `event_publication` — the table Spring Modulith 2.1 expects on PostgreSQL, hand-written.
- A multi-stage `Dockerfile` on the same template as the API's: layered jar, non-root user, readiness
  probe. Its build context is `triage/` itself, with its own wrapper, so the image is built from the
  service and nothing else.
- `docker-compose.yml` gains `postgres` (17) and `triage`. `.env.example` gains the triage database
  credentials; the platform client and the model key arrive with the steps that use them.
- The CI workflow gains a `triage` job running `./gradlew check` in `triage/`, and the image job builds
  both images.
- The ArchUnit rules of the service wait for step 2. With no domain yet they would find nothing to
  check, and a rule that passes because it found nothing proves nothing (R0.16).

**Check:** `docker compose up -d --wait` leaves the five containers healthy, and triage migrates an
empty PostgreSQL.

## Step 2 · Consuming the feed

- `ExcursionFeedListener` on `coldchain.telemetry.excursion.v1`, group `coldchain-triage`,
  `auto-offset-reset: earliest`, so a new deployment can read the history.
- It deserializes into its own `ExcursionFeedMessage`: type headers ignored, unknown fields ignored.
- **Transaction 1, `RecordExcursionEventUseCase`:**
  - insert into `inbox_event`; if the event is already `PROCESSED`, stop there;
  - apply the event to `ExcursionAlert`. The domain orders by `occurredAt` within the excursion, so a
    close that arrives before its open is recorded, and the late open only fills in what was missing;
  - on `ExcursionOpened`, an immediate rule-based triage.
- The ArchUnit rules land with the first domain: the domain imports neither Spring nor JPA, no use
  case sees a `JpaEntity`, and each suffix lives where its role says.
- `RuleBasedTriage` is pure Java:
  - severity from the duration until the context brings the thresholds (step 5): an open excursion is
    `MEDIUM`, a closed one `LOW` under 15 minutes, `MEDIUM` under 30 and `HIGH` from there;
  - cause `UNKNOWN`;
  - a summary from a template in the three languages.
- **Errors:**
  - the value is read as a string and parsed by the channel: a message that does not parse, or lacks a
    field its type requires, is dead-lettered with no retry;
  - `DefaultErrorHandler` with exponential backoff, three **blocking** attempts;
  - then `coldchain.triage.excursion-dlt.v1`;
  - acknowledgement per record, after success.

In Compose, triage starts after the API, which is what creates the feed topic it subscribes to.

**Check:** the same event delivered twice leaves one alert, and a close delivered before its open
leaves a closed alert.

## Step 3 · The platform credential — in the API

What [ADR-011](adr/ADR-011-platform-credential-for-service-calls.md) decides, in `identity`:

- the client and its Argon2id hash read from configuration;
- the token from `POST /v1/auth/client-token` with no organization and the single scope
  `PLATFORM_EXCURSION_READ`, which no role or `api_client` can be granted;
- a security filter chain for `/internal/**`, ordered before the public one;
- `/internal/**` left out of the published OpenAPI document;
- issuing the platform token logged, not audited: `AUDIT_ENTRY` belongs to an organization, and the
  platform belongs to none.

**Check:** an organization's token on `/internal/**` gets `403`; the platform token gets `403` on
every published route, walked one by one from the handler mappings. Both are integration tests, and
rule R9.19 cites them.

## Step 4 · The excursion context — in the API

`GET /api/internal/v1/shipments/{shipmentId}/excursions/{excursionId}/context`, for the platform
token only.

- It lives in `shipment`, which already depends on `telemetry::api` and owns custody and the frozen
  thresholds: `ShipmentApi.excursionContextOf(shipmentId, excursionId)`. The controller sits with the
  other shipment controllers in `delivery/web/shipment`, because controllers are grouped by module.
- `telemetry` adds `TelemetryApi.excursionWindowOf(shipmentId, excursionId, margin)`: the excursion and
  its readings in the window, read for the platform. There is no viewer, because the check happened at
  the filter chain. An excursion asked for under another shipment is `EXCURSION_NOT_FOUND`, exactly as
  if it did not exist.
- Custody at a moment is read from the custody log by `CustodyHistory`, a pure domain model with its
  own unit test. The participations come from every participant row, revoked ones included: a carrier
  that left the shipment afterwards still held it while it held it.
- The response carries:
  - the excursion: kind, opened, closed, peak, duration;
  - the readings from thirty minutes before it opened to thirty minutes after it closed;
  - the frozen thresholds;
  - the **participation** of whoever held custody when it opened (`SHIPPER`, `CARRIER`,
    `CONSIGNEE`), the minutes since the last accepted handoff, and the handoffs inside the window,
    each by participation.
  
  No organization names and no identifiers beyond the two in the path.
- A new row in the contract snapshot (R15): the internal contract versions like the public one.
- The integration profile configures the platform client, so the HTTP journey calls the internal
  endpoint too and R14.6 counts `/internal/v1/` routes among the ones that must answer.

**Check:** an integration test against Oracle returns the context of the walkthrough's excursion, with
the carrier holding custody.

## Step 5 · Asking for the context

- `ColdChainApiClient` implements `ExcursionContextSource` with `RestClient`.
- It obtains the platform token, keeps it until a minute before it expires, and asks for a new one
  instead of retrying a `401`: one fresh token and one more request.
- Around the call:
  - a timeout of two seconds, in the HTTP client: it abandons the socket, where a Resilience4j time
    limiter would only abandon a thread;
  - Resilience4j: three attempts with exponential backoff, on connection errors and `5xx` only;
  - a Resilience4j circuit breaker over the last ten calls, open for thirty seconds once half of them
    failed, so a stopped API costs no time per excursion.
  - A `404` is an answer, not a failure: it is neither retried nor counted by the breaker.
- **No context is not a failure.** Triage goes on without it and records `context_available = false`.
  Without a configured secret it never asks.
- The flow splits around the call, so no transaction waits on the network:
  - transaction 1 records the closing event as `RECEIVED` and applies it to the alert;
  - the context is asked for with no transaction open;
  - transaction 2 saves the triage and marks the event `PROCESSED`.
  A late `ExcursionOpened` for an alert already closed takes the same path, because it brings the peak
  the first triage did not have.
- With the context, the rules weigh the duration against the shipment's frozen single-excursion
  allowance: `HIGH` from the allowance, `MEDIUM` from half of it. Without it, the allowance is thirty
  minutes, which is the rule of step 2.
- Compose passes `PLATFORM_CLIENT_SECRET` to triage and only its Argon2id hash to the API.
- The integration tests stand up a fake API next to the containers: it answers the token and the
  context, refuses a wrong token, and can be down for one shipment.

**Check:** with the API stopped, an excursion still becomes an alert, with `context_available = false`.

## Step 6 · The model

- First, the version check: the Spring AI line that supports Spring Boot 4.1. If none does yet,
  `TriageAdvisor` is implemented with Anthropic's official Java SDK behind the same port, and
  nothing else changes.
- **Outside any transaction, `TriageExcursionUseCase`, for `ExcursionClosed` only:**
  - context first, then `TriageAdvisor`, with its own timeout;
  - structured output into a record that is validated: closed sets, confidence between 0 and 1,
    the three summaries present. An answer that fails validation is discarded, never repaired.
- **The rules answer whenever the model does not:** timeout, refusal, invalid output, or no key
  configured. `source` says which one answered.
- The prompt is `src/main/resources/prompts/triage-v1.st`. It holds measurements and the
  participation of the custody holder, and nothing that names a person or an organization.
- **Transaction 2:** save the triage with its provenance (model, prompt version, tokens, latency),
  and mark the inbox event `PROCESSED`.
- Tests stub the chat model. CI never calls one.

**Check:** with a key, the walkthrough's excursion comes back with `source = LLM`; without one, with
`source = RULES`, and the rest of the pipeline cannot tell the difference.

## Step 7 · Publishing the alert

- `AlertTriaged` is published from transaction 2 through the outbox: Spring Modulith's registry on
  PostgreSQL, externalized to `coldchain.triage.alert.v1`, keyed by `shipmentId`.
- The single-threaded externalization that keeps the API's feed in order applies here too, for the
  same reason ([ADR-009](adr/ADR-009-kafka-for-integration-events.md)).
- **If the process dies between transaction 1 and transaction 2**, the offset is not committed and
  Kafka redelivers the event. The inbox says `RECEIVED`, not `PROCESSED`, so the triage runs again.
  The API's consumer keeps the newest triage, so a second publication is harmless.

## Step 8 · Evaluation

- A golden set of about twelve synthetic excursions, one or two per cause:
  - a slow ramp is a refrigeration failure;
  - a short spike is a door opening;
  - a jump minutes after a handoff is handling during the handoff;
  - flat or impossible readings are a sensor fault;
  - a blackout followed by a jump is a sensor fault too.
- `./gradlew evalTriage`, in `triage/`, runs the set against the real model, by hand and outside CI, and
  prints the hits per cause and the confusion matrix.
- The result goes in the triage README, the misses included. A change of model or prompt reruns it
  before it merges.

## Step 9 · The tests that close the milestone

- **Unit, no Spring:**
  - `ExcursionAlert`: ordering by `occurredAt`, a close before its open, the late open;
  - `RuleBasedTriage`: severity at each boundary;
  - the validation of the model's answer: a cause outside the set, a confidence of 1.2, a missing
    summary.
- **Integration (Testcontainers, Kafka + PostgreSQL, the API stubbed over HTTP):**
  - an event becomes an alert published on `coldchain.triage.alert.v1`;
  - the same event twice is one alert;
  - a close before its open;
  - a malformed message on the dead letter topic, with the next valid one still consumed;
  - the API down means an alert without context;
  - the model failing means an alert from the rules;
  - an event `RECEIVED` but not `PROCESSED` is triaged again on redelivery.
- **In the API:** the two directions of the internal filter chain, and the context endpoint against
  Oracle.

**Milestone done when:** `docker compose up -d --wait` and `scripts/demo.sh` end with the alert for
the delivered shipment printed by the API, with `source = RULES` and no key configured, and with
`source = LLM` once `ANTHROPIC_API_KEY` is set; `./gradlew check` in `triage/` is green;
and with the API stopped while triage handles an excursion, the alert still reaches
`coldchain.triage.alert.v1`, with `context_available = false`.
