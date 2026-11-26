# ADR-010 — A second service for excursion triage

- **Status:** Accepted
- **Date:** 2026-11-25

## Context

[ADR-009](ADR-009-kafka-for-integration-events.md) put the excursions on a Kafka feed, and nobody
reads it yet. The first reader the platform wants is **triage**: for every excursion, a probable
cause, a severity and a short summary that each participant can act on, in their own language.

Writing that summary takes a language model, and a language model is a different kind of
dependency from anything the monolith calls today:

- **It is slow.** Seconds per call, not milliseconds.
- **It fails in new ways.** Timeouts, refusals, rate limits, and output that does not parse.
- **It costs money per call**, and its answer can change with the model or the prompt.

[ADR-006](ADR-006-modules-and-events.md) rejected microservices for the domain, and that reasoning
still holds: the five modules share a transaction where it matters, and splitting them would move
the hardest consistency problems across a network. The question here is narrower. Where does work
run that needs none of that transaction and fails in ways the custody API must never feel?

## Decision

**Triage runs in a second service, `coldchain-triage`. The domain stays a modular monolith.**

- **Why a service and not a sixth module.**
  - It fails differently: a hung model call must not hold a thread, a connection or a deployment
    of the API that records custody.
  - It scales differently: by Kafka partitions, not by HTTP requests.
  - It shares no transaction with anything: it reads a feed and writes its own rows.
  - It changes on its own cadence: a new prompt or model is a deployment of triage, not of the API.
- **It has no business HTTP surface**, only the actuator.
  - It reads `coldchain.telemetry.excursion.v1`.
  - It asks the API for the context of each excursion with a platform credential
    ([ADR-011](ADR-011-platform-credential-for-service-calls.md)).
  - It publishes `AlertTriaged` on **`coldchain.triage.alert.v1`**, keyed by `shipmentId`.
- **The API stays the only door a customer knocks on.** It consumes the alerts into a new module,
  `advisory`, and serves them with visibility by participation
  ([ADR-003](ADR-003-visibility-by-participation.md),
  [ADR-012](ADR-012-triage-is-advice-not-evidence.md)). Triage never validates a customer token and
  never learns who takes part in a shipment.
- **It owns its database: PostgreSQL.**
  - Nothing else reads it. It holds an inbox, the alerts and the publications still pending.
  - A second Oracle would be a second 2 GB process for a worker that writes a few rows per
    excursion. A second schema in the same Oracle would tie two lifecycles to one edition ceiling.
- **Same repository, independent build.** Triage lives in `triage/` with its own `settings.gradle`,
  its own Gradle wrapper and its own Dockerfile, built from `triage/` as its context. It could move to
  a repository of its own without touching a line. The API's build and its rule catalogue do not see
  it.
- **No shared library.** Each consumer declares its own record of the message and reads it
  tolerantly: a field it does not know is ignored. The contract is the JSON on a versioned topic, as
  ADR-009 decided, never a jar both sides import.
- **Inside, it has the shape of one API module** ([ADR-007](ADR-007-hexagonal-module-internals.md)):
  - a pure-Java domain with its ports, use cases behind a facade, and adapters per technology;
  - a small set of ArchUnit rules of its own enforces that shape.
- **Reliability is decided up front:**
  - **An inbox keyed by `eventId`.** Duplicates are dropped, and an event received but not finished
    is processed again when Kafka redelivers it.
  - **Blocking retries with exponential backoff.** Retry topics would break per-shipment order.
  - **A dead letter topic, `coldchain.triage.excursion-dlt.v1`, only for messages that can never be
    processed.** A model or an API that is down is not a reason to dead-letter: triage classifies
    without them.
  - **Its own outbox.** Spring Modulith's event publication registry, this time on PostgreSQL.
    Triage never writes to Kafka inside a database transaction.

## Consequences

- **Two deployables, two databases, two CI jobs.** That is the price, and it is paid once.
- **The alert is eventually consistent.** It lands seconds after the excursion, never in the same
  request. The walkthrough waits for it with a timeout instead of expecting it immediately.
- **Triage being down delays alerts and loses none.** Its consumer offsets wait in Kafka, and the
  feed's retention is far longer than any outage worth planning for.
- **The API being down degrades the alert, not the pipeline.** Triage classifies without context
  and says so on the alert.
- **Order is the consumer's job too.** A close can reach triage before its open after a retry
  (ADR-009). The domain orders by `occurredAt` within an `excursionId`, not by arrival.
- **The rule catalogue covers the API only.** Triage's rules are fewer and live in its own build. A
  rule that matters to both is written twice, on purpose: sharing it would mean sharing code.

## Alternatives considered and rejected

- **A sixth module inside the monolith calling the model.** It is the cheapest option, and it puts
  the model's failure modes inside the custody API's process, pool and deployment. Scaling triage
  would mean scaling the API.
- **Extracting `telemetry` as a service.** ADR-006 rejected it: it moves the hardest consistency
  problem across a network and buys nothing a customer can see.
- **Triage with its own HTTP API for customers.** It would have to validate the API's tokens and
  know who takes part in every shipment: a second copy of identity and visibility, and the place
  where the two copies drift apart.
- **Carrying the context inside the event.** `telemetry` cannot see custody, because the dependency
  runs the other way. The context would also bloat a contract that ADR-009 kept small on purpose.
- **A shared jar of message types.** It couples the deployments of both services to one release,
  the distributed monolith in miniature.
- **Non-blocking retry topics.** They unblock the partition by reordering it, and per-shipment order
  is the one guarantee the feed gives.
