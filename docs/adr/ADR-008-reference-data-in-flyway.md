# ADR-008 — Reference data ships in Flyway, never in an application seeder

- **Status:** Accepted
- **Date:** 2026-09-16

## Context

Some rows are not user data. The five system roles and their scopes, the reserved
`storage_profile` used by the demo, the finding codes — these are catalogue: the application does not
work without them, and nobody is expected to type them in.

There are two habits for getting them in:

**An application seeder** — a `CommandLineRunner` or `@PostConstruct` component that checks whether the
rows exist on startup and writes them if they do not. It is convenient: the data lives in Java next to
the enum it mirrors, and you can express conditions in real code.

**A Flyway migration** — the rows are `INSERT` statements in a versioned, checksummed script, applied
once and recorded in `flyway_schema_history` like any other change.

The seeder looks harmless until it scales. In a modular system a central seeder writes into tables that
belong to several different modules, which is precisely the data ownership that
[ADR-006](ADR-006-modules-and-events.md) exists to protect: the boundary holds in the code and leaks at
startup. It also makes "what is in the database" depend on which version of the application last booted
against it, rather than on a number you can read. Two environments on the same schema version can hold
different rows, and nothing anywhere records that.

## Decision

**Reference data is inserted in the Flyway migration that creates its table**, in the same script.

- The five system roles and their scopes are seeded in `V1__identity.sql`, next to the tables they
  populate.
- No `CommandLineRunner`, no `ApplicationRunner`, no `@PostConstruct` writes rows. Ever.
- Reference rows are inserted with a literal UUID v7, written out in the script. They are stable
  identifiers that tests and fixtures may reference by value. `SYS_GUID()` is not used, here or
  anywhere ([ADR-002](ADR-002-uuid-v7-raw16.md)).
- When a catalogue mirrors an enum in the code — the scope catalogue does — **the code is the source of
  truth for the set, the migration is the source of truth for the rows**, and an integration test
  asserts the two agree. A mismatch fails the build; it does not get silently repaired at boot.
- If a future catalogue genuinely needs to be synchronised at runtime, the synchroniser lives **inside
  the module that owns the table**, never in a central bootstrap package, and it only ever writes to
  that module's own tables.

## Consequences

- The database is a pure function of its schema version. `flyway_schema_history` answers "what rows are
  in there and since when", and two environments on the same version hold the same catalogue.
- Tests get a populated catalogue from the migrations alone, with no Spring context. The integration
  tests in [plan 01](../plan-01-identity.md) can assert against role codes without booting the
  application.
- Changing a reference row means writing a new migration. That is more friction than editing a Java
  list, and it is the point: a catalogue change is a schema change and gets reviewed as one.
- Conditional logic is not available. An `INSERT` cannot ask questions. For reference data, that has not
  been a limitation — if it ever is, it is a sign the row is not reference data.
- The seed statements are SQL, so they carry the Oracle rules of the project: `NUMBER(1)` booleans,
  `TRIM` checks, and no reliance on the empty string being distinct from `NULL`.

## Alternatives considered and rejected

- **A central `bootstrap/seed` package with one seeder per catalogue.** The shape the sibling system
  uses, and the reason it is rejected: its central runner writes into the tables of four different
  modules, so module data ownership is enforced everywhere except at startup. An exception that big
  is not an exception, it is the rule with extra steps.
- **A seeder per module, owned by the module.** Fixes the ownership problem and keeps the convenience.
  Rejected because it leaves the harder half untouched: the rows still depend on which build last
  booted, and no version number records them.
- **`data.sql` / `import.sql`.** Unversioned, unchecksummed, and silently skipped depending on
  `ddl-auto` — which this project sets to `none` anyway ([ADR-001](ADR-001-oracle-23ai.md)).
- **Seeding from the integration test fixtures.** Would keep production free of catalogue rows, which
  is not a feature: production needs the roles to exist.
