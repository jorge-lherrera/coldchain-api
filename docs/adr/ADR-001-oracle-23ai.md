# ADR-001 — Oracle 23ai Free as the only database, schema owned by Flyway alone

- **Status:** Accepted
- **Date:** 2026-08-27

## Context

This is a personal project, but the database I want to show mastery of is Oracle: it is the one I
maintain every day and it is where the traps live that never show up in a tutorial. A portfolio project
built on PostgreSQL would be more comfortable to stand up and would demonstrate none of that.

At the same time, the repository has to be clonable and runnable with no licenses and no paperwork, and
the tests have to run against the same engine as production. Having both at once — real Oracle and a
frictionless start — has been possible ever since the **Free** edition and `gvenzl`'s container images.

## Decision

- **Oracle Database 23ai Free** as the only engine, in development and in tests.
- The schema is governed by **Flyway alone**. `ddl-auto` is off in every profile, including the test
  ones: every table, index and `CHECK` is born in a versioned, reviewable migration.
- Integration tests spin up **Testcontainers** with `gvenzl/oracle-free:23-slim-faststart`, reusing the
  container across classes.
- Oracle's idioms are assumed in the design from the start, not retrofitted afterwards.

## Consequences

- **There are no partial indexes.** All conditional uniqueness is solved with a function-based unique
  index — `CASE WHEN cond THEN col END` — exploiting the fact that Oracle does not index `NULL`s. That is
  the piece that guarantees "one active assignment per device" and "one pending handoff per shipment".
- **Booleans are `NUMBER(1)`** with `CHECK (col IN (0,1))`. The native `BOOLEAN` in 23ai does exist, but
  it still springs surprises on indexes and drivers, and this project is not the place to break it in.
- **The empty string is `NULL`.** Mandatory fields are defended with `NOT NULL` plus
  `CHECK (TRIM(col) IS NOT NULL)`, never by comparing against `''`.
- **`JSON` and `CLOB` columns cannot go into `DISTINCT`, `GROUP BY` or an equality predicate**: Oracle
  answers ORA-22848 at run time, not at compile time. Variable payloads are read by key only.
- **`TRUNC` on a `TIMESTAMP` returns a `DATE`** and takes the fractional second with it. The downsampling
  grouping does the `CAST` explicitly so that the loss is a decision.
- The Oracle image weighs around 2 GB and takes tens of seconds to start: integration tests are slow by
  design and live in a Gradle task separate from the unit tests.
- The Free edition caps out at **12 GB of user data and 2 GB of RAM**. The demo's series generator works
  inside that ceiling on purpose.
- Partitioning is included in Free, so `temperature_reading` can be range-partitioned with a monthly
  interval.

## Alternatives considered and rejected

- **PostgreSQL.** More comfortable (partial indexes, `boolean`, `jsonb` in any query) and that is exactly
  why it is no good: the goal is to demonstrate Oracle, and its limitations are the content.
- **H2, or Testcontainers with PostgreSQL in the tests.** They would hide every trap above until
  production. A test that passes against an engine that is not the production one is green by absence.
- **`ddl-auto: update` in development.** It produces a schema nobody has reviewed and that does not match
  the one being deployed. The schema is production code.
