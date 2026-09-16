# Build plan · 05 · compliance

Two tables and the payoff of the whole story: turning a temperature series into a verdict somebody can
defend. The milestone ends when the `README` demo runs end to end and the certificate comes out `FAIL`
with its two findings.

It depends on `shipment` (closing the shipment) and on `telemetry` (the readings and the excursions),
and on neither one by query: only by event and through their `api/` (R1.13). Module layout:
[ADR-007](adr/ADR-007-hexagonal-module-internals.md).

## Step 1 · Migration V5 — the two tables

- `compliance_certificate` and `certificate_finding`, with the four audit columns (R6.15).
- `UNIQUE (shipment_id, version)`, and **a single current version per shipment** with the usual
  function-based unique index (R6.10):

  ```sql
  CREATE UNIQUE INDEX UQ_CERTIFICATE_CURRENT
    ON COMPLIANCE_CERTIFICATE (CASE WHEN SUPERSEDED_AT IS NULL THEN SHIPMENT_ID END);
  ```

- `threshold_snapshot` and `detail` are `JSON`. **They are read by key only**: they cannot appear in
  `DISTINCT`, `GROUP BY` or an equality predicate, or `ORA-22848` shows up at runtime (R6.8).
- `CHECK` on `verdict`, `severity` and `code` — closed sets that change with a deployment (R6.13).
- `CHECK (coverage_pct BETWEEN 0 AND 100)`. Percentages and temperatures are `NUMBER(5,2)`, durations
  are whole seconds (R4.9).
- Index on `shipment_id`, and on `verdict` for the dashboard.

**Check:** two current certificates for the same shipment fail with `ORA-00001`; once the first one is
marked as superseded, the second one goes in.

## Step 2 · Computing the verdict

The heart of the module, and **the flagship case for the whole architecture**: `VerdictCalculator` is a
domain service in `internal/domain/service` that takes the frozen thresholds, the reading series and
the excursions, and returns a verdict with its findings. It has **no repository, no Spring and no JPA**
— which is exactly why its tests are written first and run in milliseconds
([ADR-007](adr/ADR-007-hexagonal-module-internals.md)).

In: the shipment's frozen thresholds, the series of readings, the excursions and the window
`[evaluated_from, evaluated_to]`. Out: the verdict and its findings.

1. **Coverage** = readings received / readings **expected**, and the expected ones come from the
   device's declared sampling interval times the duration of the window. Counting against the ones
   received would always give 100% and would measure nothing.
2. **Cumulative excursion** = the sum of the durations of every closed excursion of that shipment.
3. **Longest stretch** = the largest of those durations. It is compared against
   `max_single_excursion_min`, which is a different limit from the cumulative one: six ten-minute
   stretches and one sixty-minute stretch are not the same thing even if they add up the same.
4. **Calibration** — if it expired during the journey, the verdict cannot be better than
   `PASS_WITH_DEVIATION`: the measurement exists, but it is not defensible.

The verdict comes from applying, in this order:

- coverage below the minimum → **`FAIL`** with a `DATA_GAP` finding;
- cumulative excursion or longest stretch above what is tolerated → **`FAIL`** with the matching
  excursion finding;
- any excursion within tolerance, or expired calibration → **`PASS_WITH_DEVIATION`**;
- none of the above → **`PASS`**.

Two rules that are not up for negotiation:

- **It is computed from the shipment's `threshold_snapshot`**, never by reading the current profile
  from the catalog.
- **Without minimum coverage there is no `PASS`, not even with zero excursions.** Not measuring is not
  complying; otherwise turning the sensor off would be enough to pass.

## Step 3 · The findings

- One finding per fact, each one pointing at whatever produced it: `excursion_id` when there is one,
  and `detail` in JSON with the concrete numbers.
- Codes: `EXCURSION_ABOVE_MAX`, `EXCURSION_BELOW_MIN`, `DATA_GAP`, `CALIBRATION_EXPIRED`,
  `MISSING_HANDOFF_ACCEPTANCE`, `NO_DEVICE_ASSIGNED`.
- **A verdict without traceable findings is not issued.** A `FAIL` that cannot say why is no use for
  arguing with anyone, which is exactly what the certificate exists for.
- `MISSING_HANDOFF_ACCEPTANCE` comes from the custody log, read through `ShipmentApi`, not from
  telemetry: there was a leg of the journey with nobody accepting custody.

## Step 4 · Domain and persistence

Two aggregates in the four-artefact shape: `ComplianceCertificate` and `CertificateFinding` as pure
Java in `internal/domain/model`, their `<X>JpaEntity`, their `<X>PersistenceMapper` with round-trip
tests, and their `<X>RepositoryAdapter` behind a port in `internal/domain/repository`.

**The certificate is immutable, so there is no update use case at all** — not one
([ADR-005](adr/ADR-005-immutable-certificate.md)). The domain model has no setters and no mutating
method; a new fact produces a new instance and a new row.

## Step 5 · Issuing and reissuing

`internal/application/usecase/command/`:

1. `IssueCertificateUseCase` — triggered by the `ShipmentClosed` event. It computes, writes version 1
   and its findings. It runs **inside the publishing transaction** (R10.3): if the certificate cannot
   be issued, closing the shipment fails.
2. `ReissueCertificateUseCase` — triggered by `BackfillIngested` when readings arrive for an already
   closed shipment. It computes again with the complete series, marks the previous version superseded
   and writes N+1. The handler is idempotent (R10.4).
3. **There is never an `UPDATE` on an issued certificate.** The previous version is not deleted: it
   stays superseded and queryable, because somebody may have made a decision based on it.

`internal/application/usecase/query/`:

4. `GetCertificateUseCase` — the current version by default, any version by number, and the full
   history.

Both event handlers live in `internal/infrastructure/messaging`; the events themselves belong to the
emitting module's `api/event` and are imported from there (R10.5).

## Step 6 · Manual review

- `ReviewCertificateUseCase` — a quality role reviews a certificate: it leaves `reviewed_by` and
  `reviewed_at`.
- **The review does not change the verdict.** If it could, the computation would be pointless and the
  certificate would go back to being somebody's word. The domain model offers no way to set it.

## Step 7 · Downloads with a hash

- **PDF** of the certificate and **CSV** of the complete series, both with their `content_hash`.
- The hash is computed over the bytes of the document and stored at issue time: it is what makes it
  possible to verify months later that the PDF somebody is showing is the one that was issued.
- The CSV is generated **in streaming** and never loads the series into memory (R7.3): a long shipment
  is a hundred thousand rows.

## Step 8 · The dashboard

Four numbers, all over the shipments the organization can see:

- compliance rate (`PASS` over the total),
- excursions per carrier,
- average coverage,
- average time in transit.

The queries aggregate **in the database**, not by pulling rows into Java (R7.3). And **none of them
touches the `JSON` columns** in the `GROUP BY` (R6.8).

## Step 9 · Presentation

Controllers in `delivery/web/compliance`, depending only on `ComplianceApi` (R1.16, R5.5). Downloads
are the one place a controller answers outside the `ApiResponse` envelope, and they do it by returning
a typed file response rather than assembling anything by hand (R8.10).

## Step 10 · The tests that close the milestone

- **Unit tests of `VerdictCalculator`**, which are the important ones and need no database:
  - clean, complete series → `PASS`;
  - complete series with a 10-minute excursion and a tolerance of 30 → `PASS_WITH_DEVIATION`;
  - complete series with a 40-minute excursion and a tolerance of 30 → `FAIL`;
  - six 10-minute excursions with a cumulative tolerance of 30 → `FAIL` on the cumulative total
    even though none of them exceeds the maximum stretch;
  - **series with a gap and not a single excursion → `FAIL` with `DATA_GAP`** — the case that proves
    the two rules are independent;
  - calibration expired halfway through the journey and everything else correct →
    `PASS_WITH_DEVIATION`.
- **Integration (Testcontainers):**
  - closing a shipment issues version 1 with its findings;
  - a late batch issues version 2 and leaves version 1 superseded but still queryable;
  - two current versions of the same shipment fail in the database;
  - the manual review does not alter the verdict;
  - the hash of the downloaded PDF matches the stored one.
- **Architecture:** `@ApplicationModule(allowedDependencies = {"shared", "shipment::api",
  "telemetry::api"})`, verified by Modulith (R1.5); ArchUnit adds that `compliance` never queries
  another module's tables and that `VerdictCalculator` depends on no repository.

**Milestone closed:** the full `README` walkthrough — the lab dispatches, the carrier accepts the
handoff, the gateway pushes the series with the spike and the gap, the hospital accepts the delivery —
and the certificate comes out `FAIL` with `EXCURSION_ABOVE_MAX` and `DATA_GAP`, with its verifiable
hash, and no rule of the `compliance` area still `pending` (R14.3).
