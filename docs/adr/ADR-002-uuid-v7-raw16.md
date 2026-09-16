# ADR-002 — UUID v7 in `RAW(16)` as the primary key

- **Status:** Accepted
- **Date:** 2026-08-27

## Context

The shape of the primary key has to be chosen before the first migration is written, because changing it
later touches every table and every API contract.

Two of the system's tables grow with no ceiling — `temperature_reading` and `custody_event` — and are
written in bursts. In those two, the shape of the key decides whether the index fragments or not.

On top of that, identifiers travel in the URL and are seen by different organizations: a sequential key
would tell a carrier how many shipments the laboratory moves, which is business information that is none
of its business.

## Decision

- Primary key is a **UUID v7 generated in the application**, stored in a **`RAW(16)`** column.
- `SYS_GUID()` is not used as a key: it is not a UUID, it does not preserve temporal ordering and it ties
  generation to the database. It is reserved, at most, for data seeds.
- The external representation is the canonical hyphenated one; the conversion lives in a single
  converter, not scattered across the mappers.

## Consequences

- v7 carries the creation instant in its high bits, so inserts **land at the end of the index** instead
  of scattering across it: that is what makes it viable to insert readings in batches without
  fragmenting the B-tree.
- It takes **16 bytes** instead of the 36 of a `VARCHAR2(36)`, and it compares as binary rather than as
  text. On the big tables and their indexes the difference is real.
- Keys can be generated **before** going to the database: a batch of a thousand readings goes in as a
  single batch operation, with no round trip per row to ask for the identifier.
- The v7 has to be **generated in Java**: `java.util.UUID` does not produce it, so the project needs its
  own generator (some twenty lines, with a monotonicity test) or a dependency.
- The identifier **reveals roughly its creation time**. Accepted: that is not a secret, and the business
  volume — which was one — stays hidden.
- In any native query, a `RAW(16)` parameter will not compare against a string. Mistakes of this kind are
  silent: they return zero rows instead of failing, so every repository with native SQL carries its own
  integration test.

## Alternatives considered and rejected

- **A numeric sequence.** It leaks business volume to the other organizations, forces a trip to the
  database before the identifier is known, and ties the domain to the engine.
- **UUID v4.** It solves the opacity but fragments the index in exactly the two tables that grow most.
- **`VARCHAR2(36)`.** Easier to read in a hand-written query, which is why it tempts; it costs twice the
  space and turns every key comparison into a text comparison.
