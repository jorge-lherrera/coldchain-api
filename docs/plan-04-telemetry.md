# Build plan · 04 · telemetry

The only module with real algorithmic logic: work out which shipment each reading belongs to,
duplicate nothing even if the same batch arrives three times, and detect the stretches out of band.
The milestone ends when the same batch sent twice answers the same thing without writing a single
new row.

Depends on `identity` (the machine credential that authorizes ingestion) and on `shipment` (the
dispatch event). It queries neither one's tables (R1.13). Module layout:
[ADR-007](adr/ADR-007-hexagonal-module-internals.md).

## Step 1 · Migration V4 — the five tables

- `sensor_device`, `device_assignment`, `reading_batch`, `temperature_reading` and `excursion`, each
  with the four audit columns (R6.15).
- **`temperature_reading` is partitioned** by range with a monthly interval on `measured_at` (R6.16).
  It is the only table that grows with no ceiling, and partitioning is included in the Free edition:

  ```sql
  PARTITION BY RANGE (MEASURED_AT)
  INTERVAL (NUMTOYMINTERVAL(1,'MONTH'))
  ( PARTITION P_INITIAL VALUES LESS THAN (TIMESTAMP '2026-01-01 00:00:00 +00:00') )
  ```

- A device **cannot have two active assignments**. Function-based unique index (R6.10), again taking
  advantage of the fact that Oracle does not index `NULL`s:

  ```sql
  CREATE UNIQUE INDEX UQ_ASSIGNMENT_ACTIVE
    ON DEVICE_ASSIGNMENT (CASE WHEN DETACHED_AT IS NULL THEN DEVICE_ID END);
  ```

  This is not a check in Java: two simultaneous requests would slip right past it (R6.11).
- `UNIQUE (device_id, measured_at)` on `temperature_reading` — the other half of idempotency.
- `idempotency_key` unique on `reading_batch`.
- Composite index `(shipment_id, measured_at)` on the readings — equality first, range second (N6.2):
  it is the one that serves the time series and the certificate's coverage computation.
- `CHECK` on `status`, `source` and `kind`; `CHECK (celsius BETWEEN -100 AND 100)` to throw out an
  absurd reading from a broken sensor right at the door. Temperatures are `NUMBER(5,2)`, never a
  floating-point type (R4.9).

**Check:** insert readings from three different months and check in `USER_TAB_PARTITIONS` that Oracle
created the partitions on its own. Two active assignments for the same device: `ORA-00001`.

## Step 2 · Domain and persistence

Five aggregates, each in the four-artefact shape of [ADR-007](adr/ADR-007-hexagonal-module-internals.md):
a pure-Java model in `internal/domain/model`, a `<X>JpaEntity`, a `<X>PersistenceMapper` with its
round-trip test, and a `<X>RepositoryAdapter` implementing a port declared in
`internal/domain/repository`.

Two of them carry the module's real logic, and **both are domain services in
`internal/domain/service`** — pure Java operating on domain models, with no repository and no Spring:

- **`AssignmentWindowResolver`** — given a reading and the device's assignment windows, returns the
  shipment it belongs to, or nothing.
- **`ExcursionDetector`** — given a series and the frozen thresholds, returns the excursions it opens
  and closes.

That is what makes steps 4 and 5 testable against hand-built series in milliseconds, with no
database anywhere near them. It is the single strongest argument for the whole architecture, and this
is the module where it pays off most.

## Step 3 · Use cases

`internal/application/usecase/command/`:

1. `RegisterDeviceUseCase` — serial, model, firmware, calibration date and **declared sampling
   interval**. That interval is what later decides how many readings were expected.
2. `AssignDeviceToShipmentUseCase` — opens a `device_assignment` with `attached_at`. Fails if the
   device already has an active one, and the error comes from the database, not from an `if`: the
   adapter reads `UQ_ASSIGNMENT_ACTIVE` off the violation and throws
   `DomainException.of(TelemetryErrorCode.DEVICE_ALREADY_ASSIGNED)`.
3. `DetachDeviceUseCase` — closes the window with `detached_at`.
4. `IngestBatchUseCase` — step 4 below.

`internal/application/usecase/query/`:

5. `ListDevicesUseCase` — paginated, with status and calibration, declaring its sortable fields (R5.3).
6. `QuerySeriesUseCase` — step 6 below.

The `[attached_at, detached_at)` window is what resolves step 5. That is why it is recorded even if
it looks like paperwork.

## Step 4 · Batch ingestion, idempotent

The most demanding endpoint in the system. A gateway calls it, not a person.

1. `IngestBatchUseCase` receives the idempotency key and N readings with their timestamp, as a
   `ReadingBatchCommand` record in `api/dto`.
2. **If the key already exists**, nothing is written: the original result is returned with
   `status = REPLAYED` (R11.3). Resending is free and safe, which is exactly what a gateway with
   intermittent connectivity needs.
3. If it is new, each reading is resolved (step 5) and classified as accepted or discarded **with a
   reason**: `OUT_OF_WINDOW`, `DUPLICATE`, `IMPOSSIBLE_VALUE`.
4. The accepted ones go in **in a single batch operation**, with the UUIDs generated in the
   application (R7.4). No round trip per row: a batch of a thousand readings is one statement, not a
   thousand, and a query-budget test asserts that by exact equality (R7.2).
5. The response says how many it accepted, how many it discarded and why. A partially valid batch is
   `PARTIAL`, not an error: discarding 3 out of 1000 is no reason to throw away the other 997.
6. A batch arrives out of order and that is normal — **order is never assumed anywhere**.

## Step 5 · Which shipment each reading belongs to

The rule that is easiest to get wrong:

> A reading belongs to the shipment that had that device assigned **at its `measured_at`**, not to
> the shipment that has it now.

- `AssignmentWindowResolver` looks for the `device_assignment` whose window contains `measured_at`.
- A reading outside every window **is discarded with a reason**, not hung off the current shipment.
  That is what makes late data from an already-closed shipment end up on the right shipment and
  trigger its version of the certificate.
- Unit test with the case that exposes everything: a device that did two shipments back to back and a
  batch that arrives late with readings from both. No database: the resolver takes windows and
  returns a shipment id.

## Step 6 · Excursion detection

Runs **when each batch is closed**, not reading by reading: the logic needs to see the series.

- An excursion **opens** with the first reading out of band and **closes** with the first one back
  inside. Both stay referenced by identifier — `opened_by_reading_id`, `closed_by_reading_id` — so the
  certificate's finding can point at the concrete fact.
- While it is `OPEN`, its duration is provisional and gets recomputed on every batch.
- On opening it publishes `ExcursionOpened`; on closing, `ExcursionClosed` with the final duration.
  Both are `record`s in `api/event` (R10.5) and run inside the publishing transaction (R10.3). The
  shipment only updates its `has_open_excursion` flag.
- A late batch can **open an excursion in the middle of an already-processed series**. The algorithm
  re-evaluates the time window the batch touches, not just its tail.
- `peak_celsius` is the extreme reached; `threshold_celsius`, the threshold that was crossed. Both
  are stored so the certificate does not have to recompute them.

## Step 7 · Queryable time series

- `QuerySeriesUseCase` with aggregation by minute or by hour, so a two-week shipment does not return
  a hundred thousand rows to a chart. The aggregation happens **in the database**, not by pulling rows
  into Java (R7.3).
- The grouping does an **explicit `CAST`**: `TRUNC(measured_at,'MI')` turns the `TIMESTAMP` into a
  `DATE` and takes the fractional seconds with it. It is the Oracle trap that only shows up at
  runtime.
- Returns min, max and average per interval, plus the number of readings — which is what lets you
  spot the gap at a glance.

## Step 8 · Who can ingest

- Ingestion is authorized by **a machine credential with the ingestion scope and nothing else**
  (R9.13). A compromised gateway cannot read a single shipment: it does not have the scope, and it
  never picks its own organization — that travels signed in the token.
- The rest of the module's endpoints are user-facing, with their own scopes. Controllers live in
  `delivery/web/telemetry` and depend only on `TelemetryApi` (R1.16, R5.5).
- `reading_batch` stores which `api_client_id` sent the batch: if a sensor starts sending garbage,
  you know where it came in.
- Ingestion is rate limited (R9.14).

## Step 9 · Tests that close the milestone

- **Unit, and these are the important ones:** window resolution including the device with two
  shipments back to back; opening and closing excursions over hand-built series, including the late
  batch that opens one in the middle; the classification of discards; the five mapper round trips.
  None of them starts Spring.
- **Integration (Testcontainers):**
  - the same batch twice: the second answers `REPLAYED`, with the same result and **without a single
    new row**;
  - two readings from the same device with the same timestamp: the second is discarded as a
    duplicate;
  - two active assignments for the same device: fails in the database;
  - a batch of 5,000 readings goes in with a single operation, checked by exact query count;
  - readings from three months create three partitions;
  - the ingestion credential gets `403` when it asks for a shipment.
- **Architecture:** `@ApplicationModule(allowedDependencies = {"shared", "identity::api",
  "shipment::api"})`, verified by Modulith (R1.5); ArchUnit adds that `telemetry` imports nothing from
  `compliance` and that the two domain services touch no repository.

**Milestone done when:** a gateway sends a batch with a 38-minute spike above the maximum and a
22-minute gap; the system opens and closes the excursion, the same batch resent answers `REPLAYED`
without duplicating anything, and no rule of the `telemetry` area is still `pending` (R14.3).
