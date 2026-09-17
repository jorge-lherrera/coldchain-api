# ADR-007 — Hexagonal internals: pure domain, ports and adapters

- **Status:** Accepted
- **Date:** 2026-09-16

## Context

[ADR-006](ADR-006-modules-and-events.md) makes the five modules real boundaries. It says nothing about
what a module looks like on the inside, and that is where a codebase actually rots.

Two shapes were on the table, both of them in production somewhere:

**A five-link chain** — `Resource → Interactor → UseCase → Service → Repository`, with `I*` interfaces
in `domain/` and their implementations in `application/`, and JPA entities used directly as the domain
model. It is quick to start: one entity, one Spring Data repository, one service, done.

**Hexagonal internals** — a pure domain model, repository ports it defines, and adapters in the
infrastructure layer that implement them against JPA.

The five-link chain has two links that do not earn their keep: the interactor only forwards to the use
case, and the business logic ends up in the service anyway. More importantly, using the JPA entity as
the domain model means the business rules are written on an object whose lifecycle Hibernate owns. You
cannot instantiate an aggregate in a unit test without dragging in a persistence context, so the rules
that matter — the state machine, the verdict computation, the excursion detection — end up tested
through the database or not tested at all.

This project is a cold-chain custody system. Its value is in rules that must be provable:
`threshold_snapshot` arithmetic, an append-only hash-chained log, a state machine with six transitions.
Those rules deserve to be testable in milliseconds.

## Decision

Every module is organised as follows, and the layout is not negotiable per module:

```
modules/<module>/
  <Module>ModuleConfig.java        wiring owned by the module
  package-info.java                @ApplicationModule(allowedDependencies = {...})
  api/                             the ONLY thing other modules may import
    <Module>Api.java               the published interface
    dto/                           <X>Command, <X>Result, <X>Filter, <X>SortField
    event/                         <X>Event
    policy/                        <X>Policy
  internal/                        nothing outside the module may import this
    application/
      <Module>Facade.java          implements <Module>Api, orchestrates use cases
      mapper/<X>ApiMapper.java     domain model -> api dto
      usecase/command/             <Verb><Noun>UseCase — writes
      usecase/query/               <Verb><Noun>UseCase — reads
    domain/
      model/<X>.java               pure Java. No JPA, no Spring, no annotations
      repository/<X>Repository.java   the PORT, declared by the domain
      service/<X>Service.java      domain logic spanning several aggregates
    exception/<Module>ErrorCode.java
    infrastructure/persistence/
      adapter/<X>RepositoryAdapter.java   implements the port
      entity/<X>JpaEntity.java            the JPA mapping, and nothing else
      jpa/<X>JpaRepository.java           Spring Data
      mapper/<X>PersistenceMapper.java    domain model <-> JPA entity
    infrastructure/<technology>/          one folder per outbound technology the module speaks
```

The rules that give the layout meaning:

1. **The domain model is pure Java.** No `@Entity`, no `@Column`, no Spring. `Shipment` is a class you
   can `new` in a unit test. Aggregates are created through a named factory — `Shipment.createNew(...)`
   — never through a public no-args constructor.
2. **The port belongs to the domain, the adapter to the infrastructure.** `ShipmentRepository` is an
   interface in `internal/domain/repository` that speaks in domain models. `ShipmentRepositoryAdapter`
   in `internal/infrastructure/persistence/adapter` implements it using a Spring Data
   `ShipmentJpaRepository` and a `ShipmentPersistenceMapper`.
3. **No use case ever sees a JPA type.** If a `JpaEntity` appears in an `application/` import, the
   design has leaked.
4. **A use case is one class with one public `execute`.** Named `<Verb><Noun>UseCase`, annotated
   `@UseCase`, split into `usecase/command` for writes and `usecase/query` for reads. Constructor
   injection, written out explicitly — no field injection, no Lombok on use cases.
5. **`@Transactional` goes on the use case for writes and on the adapter for anything that must be
   atomic on its own.** Queries are not transactional unless they genuinely need a consistent read.
6. **The adapter translates database failures into domain errors.** A unique index rejecting a row
   comes back as `DataIntegrityViolationException`; the adapter inspects the constraint name and throws
   a `DomainException` carrying the module's `ErrorCode`. This is the link that makes
   [rule 4](../../CONTRIBUTING.md) — invariants live in the database — produce an RFC 7807 body with a
   stable code instead of a 500.
7. **Controllers live outside the module**, in `delivery/web/<module>`. They speak HTTP and call
   `<Module>Api`. A controller never touches a use case, a repository or a domain model directly.
8. **Cross-module calls go through `api/`.** `shipment` calling `catalog` means
   `CatalogApi.getStorageProfile(...)` returning a `StorageProfileResult` — never a query against
   `storage_profile`.

## Consequences

- An aggregate costs four artefacts: domain model, JPA entity, persistence mapper, repository adapter.
  For a table that is a plain catalogue this is real ceremony, and it is paid on every one of the 23
  tables.
- Hand-written mappers are tedious and are where a forgotten field hides. They are covered by a
  round-trip unit test per aggregate: domain → entity → domain must be an identity.
- In exchange, the rules that matter are testable without Docker. The verdict computation, the state
  machine and the hash chain are unit tests that run in milliseconds, which is why
  [plan 05](../plan-05-compliance.md) can write its tests before its endpoints.
- Swapping the persistence technology becomes an adapter, not a rewrite. That is a nice property, and
  it is **not** why this was chosen — it is chosen for testability and for the boundary.
- An `ArchUnit` test enforces every rule above. A design rule nobody checks is a comment.

## Alternatives considered and rejected

- **The five-link chain** (`Resource → Interactor → UseCase → Service → Repository`). Rejected: the
  interactor is a class that forwards, and the pairing of `I*` interfaces with a single implementation
  each adds a file per concept without adding a seam anybody uses.
- **JPA entity as the domain model.** The cheapest option, and the one that quietly makes the business
  rules untestable without a persistence context. Rejected for exactly that.
- **Spring Data repositories injected straight into the use case.** Drops the mapper and the adapter,
  and with them the seam where a constraint violation becomes a domain error. It also puts
  `Page<CompanyJpaEntity>` in the application layer, which is the leak in rule 3 wearing a hat.
- **A separate Gradle module per bounded context.** Enforces the boundary at compile time, which is
  stronger than `@ApplicationModule`. Rejected: five Gradle modules for a project one person builds is
  build-tool ceremony, and Spring Modulith already fails the build on a forbidden dependency.
