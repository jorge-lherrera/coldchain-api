# Build plan · 03 · shipment

The heart of the system: nothing moves without a shipment. Five tables, a state machine and the
visibility rule everything else hangs on. The milestone ends when two organizations hand custody
over with a code and a third one cannot see the shipment.

Depends on `identity` (organization and scopes) and on `catalog` (the thresholds that get frozen),
both through their `api/` and never by query (R1.13). Module layout:
[ADR-007](adr/ADR-007-hexagonal-module-internals.md).

## Step 1 · Migration V3 — the five tables

- `shipment`, `shipment_line`, `shipment_participant`, `custody_event` and `handoff_request`, each
  with the four audit columns (R6.15).
- The columns pointing outside the module — `origin_site_id`, `destination_site_id`,
  `consignee_org_id`, `current_custodian_org_id`, `product_id` — are `RAW(16)` **with no
  `REFERENCES`** (R4.10). Inside the module there are real foreign keys, each with an explicit delete
  action (R6.5).
- The five threshold fields on `shipment` (`min_celsius`, `max_celsius`,
  `max_cumulative_excursion_min`, `max_single_excursion_min`, `min_coverage_pct`) start out `NULL`
  and are filled in at dispatch. A `CHECK` forces all five or none, and nothing writes them twice
  (R4.12).
- `CHECK` on `status` with the six values, and on `participation` and `kind`.
- `sequence_no` unique per shipment: `UNIQUE (shipment_id, sequence_no)`. **It does not come out of
  an Oracle sequence** — a sequence leaves gaps when you roll back, and the custody log cannot have
  any.
- One single pending handoff per shipment, again with a function-based unique index (R6.10):

  ```sql
  CREATE UNIQUE INDEX UQ_HANDOFF_PENDING
    ON HANDOFF_REQUEST (CASE WHEN STATUS = 'PENDING' THEN SHIPMENT_ID END);
  ```

- Indexes on `shipment_id` in the four child tables, and on `organization_id` in
  `shipment_participant` — that one serves the "my shipments" listing (R9.7).

**Check:** inserting two `handoff_request` rows in `PENDING` for the same shipment fails with
`ORA-00001`; with the first one in `ACCEPTED`, the second goes in.

## Step 2 · The state machine, in the domain

Before any endpoint. It is the piece that defines the module, and it is the clearest illustration of
why the domain model is pure Java: **`ShipmentStatus` and the transition table live in
`internal/domain/model`, with no Spring and no JPA**, so the 36-case test runs in milliseconds with
nothing started up.

- The allowed transitions are declared **in a single place**: `DRAFT → IN_TRANSIT | CANCELLED`,
  `IN_TRANSIT → IN_TRANSIT | AT_DESTINATION`, `AT_DESTINATION → DELIVERED | REJECTED`, and the three
  terminal ones with no way out.
- An attempt at a transition that is not allowed throws a domain exception that the edge translates
  into a stable `ILLEGAL_TRANSITION` code, with the current and the requested status in the body.
- **No controller decides a transition, and no use case decides one either.** The resource calls the
  use case; the use case asks the aggregate; the aggregate accepts or rejects.
- Exhaustive unit test: the 36 combinations of source and target status, checking that exactly the
  six allowed ones go through.

The aggregate `Shipment` is created through `Shipment.createDraft(...)` (R3.3), and it is the class
that owns `dispatch()`, `arrive()`, `deliver()` and `reject()`.

## Step 3 · Creation and dispatch

All in `internal/application/usecase/command/`:

1. `CreateShipmentUseCase` — born in `DRAFT` with origin, destination, consignee and its lines. The
   organization that creates it comes in as a `SHIPPER` participant, the consignee as `CONSIGNEE`.
   Emits the `CREATED` custody event.
2. `AddLineUseCase` / `RemoveLineUseCase` — only in `DRAFT`. Each line copies the product name and the
   storage profile version: if the catalog changes tomorrow, the line still says what was shipped.
3. `DispatchShipmentUseCase` — the use case with the most rules in the whole module:
   - requires at least one line, an assigned device and a consignee;
   - reads the profile through `CatalogApi` — never by querying `storage_profile` (R1.13) — and
     **copies the five thresholds onto the shipment** ([ADR-004](adr/ADR-004-frozen-thresholds.md));
   - sets `actual_departure_at`, moves to `IN_TRANSIT`, emits the `DISPATCHED` custody event;
   - publishes `ShipmentDispatched` so `telemetry` opens its monitoring window.
   - If the profile has several versions, it freezes **the version the line references**, not the one
     active today.
4. `CancelShipmentUseCase` — only from `DRAFT`.

## Step 4 · Handoff in two steps

Custody is what the system promises to prove; it cannot change just because someone says so.

1. `OpenHandoffUseCase` — whoever holds custody generates a single-use code. **Its hash** is what gets
   stored, never the code; the code travels once in the response and cannot be looked up again.
   It expires after N minutes (configurable, 30 by default).
2. `AcceptHandoffUseCase` — the receiving organization presents the code. If the hash matches, it has
   not expired and it is still `PENDING`: `current_custodian_org_id` changes, the receiver is added as
   a `CARRIER` participant if it was not one already, and the `HANDOFF` event is emitted. The
   single-use guarantee is idempotency by key (R11.3), and the one-pending guarantee is the index of
   step 1.
3. `RejectHandoffUseCase` — closes the request without moving custody.
4. An expired handoff is marked `EXPIRED` **when it is read**, not by a scheduled job: with no
   scheduler there is one less piece to explain, and the visible result is the same.

**Custody changes only in step 2.** Opening the request moves nothing.

## Step 5 · Chained custody log

- `custody_event` is **append-only**. There is no use case that updates or deletes one.
- `sequence_no` is reserved inside the same transaction that writes the event, with a `SELECT
  MAX(sequence_no) + 1 FOR UPDATE` over the shipment's rows (R11.2). It serializes the events of a
  given shipment and leaves the sequence gap-free.
- `hash` = digest of `(sequence_no, kind, from, to, site, actor, occurred_at, previous_hash)`.
  The first one chains against a genesis constant. The digest is a domain service in
  `internal/domain/service`: pure Java, unit-tested with no database.
- `occurred_at` (when it happened) and `recorded_at` (when it was reported) are separate fields and
  both are stored. A driver with no signal syncs two hours late and that has to be visible.
- `VerifyChainUseCase` — walks a shipment's custody log and returns where it breaks, if it breaks. It
  is what makes the hash worth something in the demo.
- A past mistake **is not corrected by editing**: a compensating event is emitted, like in
  accounting.

## Step 6 · Visibility by participation

The rule from [ADR-003](adr/ADR-003-visibility-by-participation.md), and the one most easily
forgotten in a one-off query.

- A single reusable predicate: the shipment is visible if there is a live `shipment_participant`
  (`revoked_at IS NULL`) with the context organization.
- It is applied in **the repository adapter**, not in the service (R9.9): that way a new query cannot
  forget it, and the enforcement point is one place a reviewer can check.
- The paginated listing returns exactly the shipments the organization takes part in.
- Asking by identifier for a shipment you do not take part in returns **`404`, not `403`** (R9.10): a
  `403` confirms the shipment exists, and that is already leaking information to a third party.
- `AddParticipantUseCase` / `RevokeParticipantUseCase` — `SHIPPER` only. Revoking writes
  `revoked_at`, which closes a window rather than deleting a row: the history of who saw what is not
  erased (R6.9).

## Step 7 · Presentation and events

- Controllers in `delivery/web/shipment`, depending only on `ShipmentApi` (R5.5, R1.16). No entity and
  no domain model reaches a controller (R5.6).
- `ShipmentTimelineUseCase` — the full custody log, in order, with the result of the chain
  verification.
- **Events published**, as `record`s in `api/event` (R10.5): `ShipmentDispatched` (step 3) and
  `ShipmentClosed` (on delivery or rejection). They run inside the publishing transaction, so the
  response is the last thing that happens (R10.3).
- **Events consumed**: `ExcursionOpened` / `ExcursionClosed` from `telemetry`, handled in
  `internal/infrastructure/messaging`, which only update `has_open_excursion`. The handlers are
  idempotent (R10.4). **The shipment does not query the telemetry tables**: the carrier sees the
  excursion in their listing without a `join` crossing the boundary.

## Step 8 · Tests that close the milestone

- **Unit:** the whole state machine; the threshold freezing; the hash and `sequence_no` computation;
  the expiry of the handoff code; the five persistence-mapper round trips.
- **Integration (Testcontainers):**
  - two pending handoffs on the same shipment: the second fails in the database;
  - accepting with an expired code, with an already-used one and with one from another shipment: all
    three fail with different codes;
  - an organization that does not take part gets `404` both in the listing and by identifier;
  - dispatching with no lines, no device and no consignee: three different errors;
  - the hash chain verifies after five events, and stops verifying if one is altered by hand via SQL;
  - two concurrent events on one shipment do not share a `sequence_no`.
- **Architecture:** `@ApplicationModule(allowedDependencies = {"shared", "identity::api",
  "catalog::api"})`, verified by Modulith (R1.5). ArchUnit adds that `shipment` imports nothing from
  `telemetry` or `compliance` — events are the only path — and that the visibility predicate is
  applied in exactly one place.

**Milestone done when:** a `curl` creates a shipment, dispatches it, opens a handoff, accepts it from
another organization and asks for the timeline; a third organization gets `404` for that same
shipment; and no rule of the `shipment` area is still `pending` (R14.3).
