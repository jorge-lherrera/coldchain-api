# ADR-006 — Modular monolith of five modules with synchronous events

- **Status:** Accepted
- **Date:** 2026-08-27
- **Amended:** 2026-09-16 — the package layout below was made concrete, and the internals of a module
  were moved to their own decision, [ADR-007](ADR-007-hexagonal-module-internals.md).

## Context

Five modules with real dependencies between them: `catalog` feeds `shipment`, `shipment` opens
`telemetry`'s window, `telemetry` hands excursions back to `shipment` and late data to `compliance`.
Where the boundaries are and what crosses them has to be decided.

The temptation in a portfolio project is to build five services, because it sounds modern. With five
modules, a single developer and a single database, that means spending the whole effort on messaging and
deployment infrastructure instead of on the domain, which is what the project sets out to demonstrate.
The opposite temptation is a flat monolith where any class calls any other: it works right up until you
have to explain why the certificate queries the catalog.

## Decision

**Modular monolith**, a single Gradle module, three top-level packages under `com.coldchain`:

```
com.coldchain/
  ColdChainApplication.java
  delivery/          every HTTP controller, grouped by module: delivery/web/<module>
  modules/           identity · catalog · shipment · telemetry · compliance
  shared/            cross-cutting only: error model, pagination, security context, config
```

- **A module publishes `api/` and hides `internal/`.** `api/` holds the module's interface
  (`<Module>Api`), its DTOs, its events and its policies. Everything else — domain model, use cases,
  repositories, JPA entities — lives under `internal/` and is invisible to the rest of the system. The
  inside of a module is [ADR-007](ADR-007-hexagonal-module-internals.md).
- **Allowed dependencies are declared in code**, in each module's `package-info.java`:

  ```java
  @ApplicationModule(allowedDependencies = { "shared", "catalog::api" })
  package com.coldchain.modules.shipment;
  ```

  Spring Modulith verifies it. An import that is not on the list **fails the build**, and so does any
  import of another module's `internal`.
- **No `join` crosses a boundary.** References between modules are UUIDs with no foreign key, and
  reading the other side goes through `<Module>Api`.
- **Controllers are not part of a module.** They live in `delivery/web/<module>`, speak HTTP, and call
  `<Module>Api`. Keeping them out is what stops a module from being defined by its endpoints.
- Reactive communication is by **synchronous, in-process domain event**, published inside the emitter's
  transaction: `ShipmentDispatched`, `ExcursionOpened`, `ExcursionClosed`, `ShipmentClosed`,
  `BackfillIngested`. Events are declared in the emitter's `api/event`, because a consumer has to be
  able to import them.
- **`shared/` is for what has no owner** — the RFC 7807 error model, the sort catalogue, the security
  context, configuration. It never holds business logic, and a module never reaches into another module
  through it.

## Consequences

- The boundary is executable. Importing another module's `internal`, or a dependency that is not on the
  list, stops the build — not a review comment.
- Some queries need **two steps** where a `join` would have done — listing shipments with their product
  name, for instance. Accepted: it is the price of the boundary, and it is paid with one read through
  `<Module>Api`, not with an exception to the rule.
- The synchronous event **shares the emitter's transaction**. That is deliberate: if the reaction fails,
  the whole operation fails and you are not left with a dispatched shipment and no monitoring window
  open. The response to the client is the last thing that happens.
- That same transactional coupling is the limit of the model: a slow reaction would stretch the request.
  None of the five is, and if one ever were, the way out is to publish it after the *commit*, not to break
  the boundary.
- `allowedDependencies` has to be maintained by hand, and the first reflex on a build failure is to add
  an entry. Adding one is a design decision and gets reviewed as such; the list is short on purpose, and
  a module that needs three more entries is a module that is doing someone else's job.
- Extracting a module into a standalone service would be possible — the interface already exists — but
  **it is not a goal of the project** and it will not be prepared for in advance.
- Five modules and a single database schema: each module owns its own tables, and which module a
  table belongs to is recorded in the specification, not encoded in a name prefix. Nobody writes into
  another module's tables — including at startup, which is why reference data ships in migrations
  ([ADR-008](ADR-008-reference-data-in-flyway.md)).

## Alternatives considered and rejected

- **Microservices.** With five modules and one developer, the cost goes entirely into messaging,
  deployment and eventual consistency, and the domain — which is the content of the project — runs out of
  time. It would demonstrate a grip on infrastructure, not on design.
- **Flat layered monolith** (`controllers/`, `services/`, `repositories/`). It is what almost every
  portfolio project does, and it makes it impossible to explain where a module ends.
- **Five top-level packages with no `api` / `internal` split.** The boundary would exist in prose only:
  with everything public, Modulith has nothing to verify and the first deadline dissolves it.
- **Controllers inside each module.** Tempting, and it makes the module self-contained. Rejected because
  it invites a second HTTP surface per module — web, mobile, integration — to grow inside the domain;
  keeping delivery separate lets three transports share one `<Module>Api`.
- **Asynchronous events with a queue.** It adds a piece of infrastructure and eventual consistency to
  solve a problem that does not exist here: the five reactions are immediate and cheap.
