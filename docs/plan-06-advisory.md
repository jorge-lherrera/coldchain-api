# Build plan · 06 · advisory

> **Status:** in progress — opened with phase two; its code lands in step 21 of the
> [branching plan](branching-plan.md).

The sixth module, and the only one whose data does not come from the API itself: it receives the
triage that `coldchain-triage` publishes ([ADR-010](adr/ADR-010-second-service-for-excursion-triage.md))
and shows each participant of a shipment the advice about its excursions. The milestone ends when the
walkthrough prints that advice, an outsider gets a `404` asking for it, and the build refuses to let
the verdict read it.

It depends on `shipment::api` only, to decide who may see an alert, and **nothing depends on it**:
`compliance` in particular does not list it, and that absence is the point of the module
([ADR-012](adr/ADR-012-triage-is-advice-not-evidence.md)). Module layout:
[ADR-007](adr/ADR-007-hexagonal-module-internals.md).

## Step 1 · Migration V7 — the alert table

- `excursion_alert`, with the four audit columns (R6.15) and a lock version: an alert changes when a
  newer triage arrives (R11).
- `excursion_id` unique. One alert per excursion, referenced by UUID with no foreign key, because the
  excursion belongs to `telemetry` (R1.13).
- `shipment_id`, with the composite index `(shipment_id, triaged_at)`. Equality first, range second
  (N6.2); it serves the only read the module has.
- `CHECK` on the closed sets (R6.13):
  - `probable_cause` in `HANDLING_DURING_HANDOFF`, `REFRIGERATION_FAILURE`, `DOOR_OPENING`,
    `SENSOR_FAULT`, `UNKNOWN`;
  - `severity` in `LOW`, `MEDIUM`, `HIGH`;
  - `source` in `LLM`, `RULES`.
- `confidence` is `NUMBER(3,2)` with `CHECK (confidence BETWEEN 0 AND 1)`.
- The three summaries are `VARCHAR2(1000 CHAR)`, not `CLOB`. A summary is a few sentences, and a
  `CLOB` would bring `ORA-22848` with it (R6.8).
- `context_available` is `NUMBER(1)` with `CHECK (context_available IN (0,1))` (R6.7).
- `model` is nullable: a rule-based alert has none.

**Check:** two alerts for the same excursion fail with `ORA-00001`, and a `severity` of `CRITICAL`
fails the `CHECK`.

## Step 2 · Domain and persistence

`ExcursionAlert` in the four-artefact shape of [ADR-007](adr/ADR-007-hexagonal-module-internals.md):
a pure-Java model, an `ExcursionAlertJpaEntity`, a persistence mapper with its round-trip test, and an
`ExcursionAlertRepositoryAdapter` behind the port.

The one rule the module owns is a domain method, tested in milliseconds:

- **The newest triage wins.** `ExcursionAlert.reconsideredWith(triage)` replaces the advice only when
  the incoming `triagedAt` is later than the stored one. An older triage, which Kafka may redeliver
  after a newer one, changes nothing. That rule is what makes the consumer idempotent without an
  inbox.

## Step 3 · Consuming `coldchain.triage.alert.v1`

- The listener is a delivery channel, like a controller: `delivery/messaging/advisory/AlertFeedListener`
  calls `AdvisoryApi.recordAlert(RecordAlertCommand)` and knows nothing else
  ([ADR-006](adr/ADR-006-modules-and-events.md)).
- It deserializes into its own `AlertFeedMessage` and ignores fields it does not know. No type shared
  with triage ([ADR-010](adr/ADR-010-second-service-for-excursion-triage.md)).
- Consumer group `coldchain-api-advisory`; acknowledgement per record, after the write commits.
- Errors:
  - `ErrorHandlingDeserializer`, so malformed JSON goes straight to the dead letter topic;
  - `DefaultErrorHandler` with exponential backoff, three attempts;
  - then `coldchain.advisory.alert-dlt.v1`.
- The audit actor of an alert written by the listener is the system actor: no user wrote it.
- The new channel gets its rows in the rule catalogue, with their machines, in the same change:
  where a listener lives, what it may call, and the `Message` suffix.

## Step 4 · Reading it, by participation

`GET /v1/shipments/{shipmentId}/alerts`, scope `TELEMETRY_READ`, the same one the series asks for:
whoever may see the excursions may see the advice about them.

- Visibility goes through `ShipmentApi.shipmentOf(shipmentId, viewer)`: an outsider gets `404`, never
  `403` (R9.9, R9.10).
- The alerts come ordered by `triagedAt`, in the success envelope (R8.1).
- The response carries **one** `summary`, resolved from `Accept-Language` among the three stored, with
  English as the fallback. That is the same rule every message the API answers with follows.
- `probableCause`, `confidence` and `source` always travel together. The client is never shown a
  cause without how sure the triage was and where it came from.

## Step 5 · The boundary the module exists for

- `compliance` keeps its `allowedDependencies` unchanged, and Spring Modulith fails the build on any
  import of `advisory` from it.
- A rule row states it in words: **"the verdict never reads advice"**. Its enforcer is an ArchUnit
  test that names the two packages, so the reason survives even if somebody edits the Modulith
  declaration (R0.1).

## Step 6 · The tests that close the milestone

- **Unit:**
  - a newer triage replaces the advice;
  - an older one changes nothing;
  - the same one twice changes nothing.
- **Integration (Testcontainers, Oracle + Kafka):**
  - an alert published on the topic is stored;
  - the same alert published twice leaves one row;
  - an older triage published after a newer one leaves the newer;
  - a participant reads the alert, and an outsider gets `404`;
  - `Accept-Language: pt` returns the Portuguese summary;
  - a malformed message lands on the dead letter topic, and the next valid one is still consumed.
- **Architecture:** `@ApplicationModule(allowedDependencies = {"shared", "shipment::api"})`, verified
  by Modulith (R1.5); the rule of step 5 green.

**Milestone done when:** `scripts/demo.sh` prints the alert for the shipment it just delivered, with
`source`, `probableCause` and `severity`; the organization that takes no part gets `404` on
`GET /v1/shipments/{shipmentId}/alerts`; adding `import com.coldchain.modules.advisory.api.AdvisoryApi;`
to any class of `compliance` makes `./gradlew rules` fail.
