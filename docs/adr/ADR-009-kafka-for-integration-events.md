# ADR-009 — Kafka for outbound integration events

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

`telemetry` opens and closes excursions and announces them as in-process domain events,
`ExcursionOpened` and `ExcursionClosed` ([ADR-006](ADR-006-modules-and-events.md)). Inside the
monolith that is enough: the reactions are immediate, cheap and share the emitter's transaction.

Outside the monolith, nobody hears them. A carrier or a shipment owner who wants to know that a
vaccine went above 8 °C has two options today: poll the series endpoint, or wait for the
certificate when the shipment closes. Both are late, and polling does not scale with the number of
shipments.

What an outbound feed needs:

- **Durable.** An excursion that happened is not lost because a consumer was down.
- **Ordered per shipment.** `ExcursionClosed` never overtakes its `ExcursionOpened`.
- **Several independent readers.** Alerting, BI and a customer integration read the same events at
  their own pace, without knowing about each other.
- **Replayable.** A new consumer can read from the start.
- **Consistent with the database.** If the excursion is committed, the event goes out; if the
  transaction rolls back, it does not.

ADR-006 rejected a queue **between modules**, and that reasoning still holds: this decision does not
touch how modules talk to each other.

## Decision

**Excursions leave the monolith through Kafka, as integration events. Domain events stay in-process.**

- **One topic, `coldchain.telemetry.excursion.v1`**, for both `ExcursionOpened` and
  `ExcursionClosed`. They share a topic because Kafka only orders within a partition: split into two
  topics, a close could be read before its open.
- **The key is `shipmentId`.** Every event of a shipment lands in the same partition, so a consumer
  reads them in the order they happened. Different shipments spread across partitions and are
  processed in parallel.
- **The payload is a contract, not the domain record.** It is mapped to a versioned JSON message:
  `eventId`, `type`, `occurredAt`, `shipmentId`, `organizationId`, `excursionId`, `kind`, and
  `peakCelsius` or `durationMinutes`. The readings series and the thresholds do not travel: a
  consumer that needs them asks the API.
- **Publication goes through Spring Modulith's event externalization**, backed by its JPA event
  publication registry. The event is stored in the same transaction as the excursion and sent to
  Kafka after the commit: that is the outbox, and it is what makes the feed consistent with Oracle.
- **Externalization runs on a single thread.** Spring Modulith hands every event to its own
  asynchronous task, and on virtual threads the events of one commit race each other to the broker:
  a close can overtake its open, and the integration test that asserts the order caught them
  arriving shuffled. One thread sends them in the order they were published, and the idempotent
  producer keeps that order within the partition. Excursions are rare enough that one sender is
  never the bottleneck.
- **The producer runs with `acks=all` and idempotence on.** A publication is complete when every
  in-sync replica has it.
- **Delivery is at-least-once.** A consumer may see an event twice and deduplicates by `eventId`.
- **The feed is for trusted, platform-side consumers.** It carries every organization's
  excursions, so no customer reads the topic directly; delivering to a customer — a webhook, an
  email — is a consumer's job, and that consumer applies visibility by participation
  ([ADR-003](ADR-003-visibility-by-participation.md)).

## Consequences

- **A new piece of infrastructure.** Kafka joins Oracle in `docker-compose.yml`, and the integration
  tests start it with Testcontainers, the same way they start Oracle.
- **A new table, owned by Flyway.** The event publication registry needs its table, and under R13.1
  it is a hand-written migration, never generated. It has to match the schema Spring Modulith 2.1
  expects, and a completed publication is deleted as soon as Kafka confirms it, so the table only
  ever holds what is still pending.
- **Kafka being down does not stop the business.** An excursion is still committed; its event waits
  in the registry and goes out when the broker is back. The price is latency: the feed is seconds
  behind the database, never in the same request.
- **Order holds for what Kafka receives, not for retries.** If the publication of an
  `ExcursionOpened` fails and is resubmitted later, its `ExcursionClosed` can reach the topic first.
  Each event therefore carries enough to stand on its own, and consumers order by `occurredAt`
  within an `excursionId`, not by arrival.
- **The contract is versioned by topic.** A backward-compatible change — a new optional field — ships
  on `v1`. A breaking change opens `v2`, and both run until the last consumer has moved.
- **The partition count is chosen once.** Adding partitions later moves keys to other partitions and
  breaks per-shipment order at the moment of the change.
- **Locally, `acks=all` proves nothing.** One broker means one replica. Durability across brokers is a
  production concern this repository documents but does not demonstrate.

## Alternatives considered and rejected

- **Kafka between modules**, replacing the in-process events. It buys eventual consistency and a
  broker for reactions that are immediate and cheap. ADR-006 rejected it and nothing has changed.
- **SQS, or SNS fanning out to SQS.** The right tool when one consumer works through a queue, and the
  wrong one here: a read message is gone, so a new consumer cannot replay history, and each new
  reader needs its own queue wired in advance. FIFO ordering per shipment exists, at a throughput
  ceiling.
- **Publishing with `KafkaTemplate` inside the use case.** Two writes, Oracle and Kafka, with nothing
  tying them together: a crash between them loses the event, and a rollback after the send
  publishes an excursion that never existed. That is the dual-write problem the outbox exists for.
- **A transactional Kafka producer.** Its transactions cover Kafka only, not Oracle. Joining both
  would take XA, which is slow, fragile and poorly supported; the outbox reaches the same
  consistency with a table.
- **Change data capture with Debezium.** It reads Oracle's redo log and needs Kafka Connect plus
  LogMiner privileges, which is a second platform to run. It also publishes table rows, not
  business events, so the contract would be the schema.
- **Webhooks from ColdChain.** The API would own every customer's retries, back-off and downtime,
  and a slow customer would become ColdChain's problem. A webhook is a fine delivery channel —
  as a consumer of this feed, not instead of it.
- **An excursions endpoint to poll.** Simple, and always late: freshness depends on how often
  clients ask, and the load grows with the number of clients, not with the number of excursions.
- **Avro with a Schema Registry.** Stronger contract enforcement, at the cost of one more service to
  run. With one producer and consumers in the same hands, JSON plus a versioned topic is enough;
  it gets revisited when a team outside this repository consumes the feed.
