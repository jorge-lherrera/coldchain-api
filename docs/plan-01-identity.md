# Build plan · 01 · identity

Attack order for the first module. Every step leaves the project compiling; the milestone ends when
a protected endpoint answers with a token and returns `403` without the scope.

The module shape it builds on is [ADR-007](adr/ADR-007-hexagonal-module-internals.md), and what it
must satisfy is the catalogue in [`rules/project-rules.md`](../rules/project-rules.md).

## Step 0 · Repository foundations

- `gradle init` with the 9.x wrapper, Java 25 in the *toolchain*, the Spring Boot 4 plugin and the
  dependency-management one.
- Minimum dependencies: `spring-boot-starter-webmvc`, `-data-jpa`, `-security`, `-validation`,
  `flyway-core` + `flyway-database-oracle`, `ojdbc17`, `spring-modulith-starter-core`. In tests:
  `spring-boot-starter-test`, `testcontainers:oracle-free`, `archunit-junit5`.
- Two test *source sets*: `test` (unit, no Spring context) and `integrationTest` (with
  Testcontainers). Keep the slow task from sneaking into the fast one.
- `docker-compose.yml` with `gvenzl/oracle-free:23-slim-faststart`, port 1521, a named volume and the
  development password in the clear — it is local, there is nothing to protect.
- `application.yml` with `ddl-auto: none` and Flyway enabled (R13.1). A `local` profile pointing at
  the compose.
- **The package skeleton**, which is the part that decides everything after it:

  ```
  com.coldchain/
    ColdChainApplication.java
    delivery/web/              every controller, grouped by module
    modules/identity/          api/ + internal/
    shared/                    error model, response envelope, security context, pagination
  ```

- `shared/` is born in this step with the pieces every module will need: the `ApiResponse` envelope
  and its factory, the RFC 9457 problem model (`DomainException`, `ErrorCode`, `ErrorCategory`, the
  single `GlobalExceptionHandler`), and the `SortCatalog` that makes a pageable endpoint declare its
  sortable fields (R5.3).

**Check:** `./gradlew bootRun` starts up and Flyway finds no migrations to apply.

## Step 1 · UUID v7 generator

It is the piece the five migrations depend on, so it goes first and it goes with a test.

- A `UuidV7` with `next()`: 48 bits of milliseconds, 12 bits of counter for the same millisecond,
  random for the rest.
- Monotonicity test: a thousand identifiers generated within the same millisecond come out strictly
  increasing when compared as binary.
- A single `UUID ↔ RAW(16)` converter and an `AttributeConverter` for JPA. Nobody else converts
  (R4.3).

## Step 2 · Migration V1 — the identity schema

One single migration with the module's eight tables, hand-written (R13.2). What cannot be missing:

- `RAW(16)` PK, and the four audit columns on every table (R6.15).
- `organization.tax_id` unique; `CHECK` on `kind` and `status`.
- Unique index on `LOWER(app_user.email)` — not a plain `UNIQUE`.
- `CHECK (col IN (0,1))` on `role.is_system`: booleans are `NUMBER(1)`, never the native 23ai
  `BOOLEAN` (R6.7).
- `CHECK (TRIM(col) IS NOT NULL)` on every mandatory text column, because in Oracle the empty string
  **is** `NULL` (R6.12).
- Composite keys on `user_role`, `role_scope` and `api_client_scope`: the link *is* the pair (R6.2).
- `organization_id` indexes on every table that gets queried by tenancy (R9.7).
- Seed of the five system roles with their scopes, **in this same migration** and not in a startup
  seeder ([ADR-008](adr/ADR-008-reference-data-in-flyway.md)): they are catalogue, not user data.

**Check:** `./gradlew flywayMigrate` against the empty database, and once more to confirm it is
idempotent.

## Step 3 · Domain and persistence

This is the step that sets the shape every other module copies. Each aggregate is **four artefacts**:

1. **The domain model** in `internal/domain/model` — `Organization`, `AppUser`, `ApiClient`. Pure
   Java: no `@Entity`, no Spring, no annotations at all (R3.2). Created through a named factory,
   `Organization.createNew(...)`, never a public no-args constructor (R3.3). This is the class the
   business rules are written on, and the reason they can be unit-tested with no database.
2. **The JPA entity** in `internal/infrastructure/persistence/entity` — `OrganizationJpaEntity`. It
   maps columns and does nothing else. It extends `AuditableEntity` (R3.5).
3. **The persistence mapper** in `.../persistence/mapper` — `OrganizationPersistenceMapper`, domain ↔
   entity, with a round-trip unit test: domain → entity → domain must be an identity. That test is
   what catches the forgotten field.
4. **The repository adapter** in `.../persistence/adapter` — `OrganizationRepositoryAdapter`,
   implementing the port and using a Spring Data `OrganizationJpaRepository` from
   `.../persistence/jpa`.

The port itself — `OrganizationRepository` — is an interface in `internal/domain/repository` that
speaks in domain models (R2.14). No use case ever sees a JPA type.

The adapter is also where a database failure becomes a domain error: a unique index rejecting a row
arrives as `DataIntegrityViolationException`, and the adapter reads the constraint name and throws
`DomainException.of(IdentityErrorCode.EMAIL_ALREADY_REGISTERED)`. That is the link that turns an
invariant living in the database into a `422` with a stable code instead of a `500`.

The scope catalogue is a closed `enum Scope` in the code (R9.3). The table stores its name, not its
ordinal, and an integration test asserts the seeded rows match the enum (R13.5).

Every listing is `Pageable`; no query returns an unbounded `List` (R5.4).

## Step 4 · Use cases

Each one is a class in `internal/application/usecase/{command,query}` with the `UseCase` suffix,
annotated `@UseCase` and never `@Service` (R3.10), with explicit constructor injection and one public
`execute(<X>Command)` returning an `<X>Result`. The commands and results are `record`s in `api/dto`
(R3.8). Each ships with its unit test against fake repositories.

**Commands** — `usecase/command/`:

1. `RegisterOrganizationUseCase` — creates the organization and its first `ORG_ADMIN`. It is the only
   public endpoint.
2. `InviteUserUseCase` — creates the user in `INVITED` with a single-use activation token.
3. `ActivateUserUseCase` — sets the password and moves it to `ACTIVE`.
4. `AuthenticateUseCase` — validates credentials, issues *access* and *refresh*.
5. `RefreshTokenUseCase` — rotates the refresh and revokes the previous one.
6. `IssueClientTokenUseCase` — `client_credentials` for the gateways.
7. `AssignRoleUseCase` / `RevokeRoleUseCase`.

**Queries** — `usecase/query/`:

8. `GetMyScopesUseCase` — the effective scopes of the authenticated user.

`IdentityFacade` in `internal/application` implements `IdentityApi` and orchestrates them. `IdentityApi`
is what the other four modules see, and the only thing they may import (R1.10).

## Step 5 · Security

- `SecurityFilterChain` with everything closed by default and an explicit list of public routes
  declared in one place (R9.1): registration, login, refresh, activation, and `/actuator/health`.
- Session `STATELESS`, CSRF disabled — with no server-side session there is nothing to forge (R9.4).
- The token carries `sub`, `org`, `scopes` and `typ` (`user` or `client`). The filter resolves the
  organization context once and leaves it available; **no use case receives it as a parameter from
  the controller**, and no endpoint takes it from a header (R9.5).
- Scope authorization on the method, not by route in the configuration (R9.2).
- Passwords and client secrets with Argon2id (R9.11). Access token 15 minutes, refresh 7 days, and
  refresh rotation revokes the whole family on reuse (R9.12).
- Rate limiting on login, on activation and on token issuance (R9.14); the security headers of R9.15.

## Step 6 · Presentation and errors

- Controllers live in `delivery/web/identity`, **outside the module** (R1.16). They depend only on
  `IdentityApi` (R5.5), carry their own `<X>Request` / `<X>Response` records and an
  `IdentityWebMapper` between those and the module's commands and results. No entity and no domain
  model crosses to a controller (R5.6).
- `@Valid` on every request body (R5.7). Shape is validated at the edge; meaning is validated in the
  use case (R5.8).
- Success goes out through `responseFactory.ok(SuccessCode.X, ...)` — the controller never assembles
  JSON and never picks an HTTP status (R8.10, R8.11).
- Errors are the single `GlobalExceptionHandler` from `shared` (R8.9): a use case throws
  `DomainException.of(IdentityErrorCode.Y)`, `ErrorCategory` decides the status, and the body is RFC
  9457 problem+json with a stable `errorCode` (R8.2, R8.6).
- OpenAPI generated from the annotations and reachable locally (R14.5).

## Step 7 · Auditing

- A `TransactionalEventListener` writes `audit_entry` on every write, with the actor resolved from the
  security context. It runs **in the same transaction**: if the audit write fails, the operation fails
  (R9.18). Auditing is not best-effort.

## Step 8 · Tests that close the milestone

- **Unit:** the eight use cases, the UUID v7 generator, effective-scope resolution, and the
  persistence-mapper round trip for each of the three aggregates.
- **Integration (Testcontainers):** full registration, login, access with and without the scope,
  email uniqueness across two different casings (which is where the index on `LOWER` gets proven),
  refresh rotation, and the seeded scopes matching the enum.
- **Architecture:** this is the branch that delivers the first enforcers, because the whole catalogue
  starts at `pending`. At minimum `ModuleShapeArchTest`, `ModuleBoundariesArchTest`,
  `ModulithVerificationTest` and the `identity` half of `HttpContractArchTest` and
  `SecurityPostureArchTest`. Boundaries are declared in `package-info.java` with
  `@ApplicationModule(allowedDependencies = {"shared"})` and verified by Spring Modulith (R1.5);
  ArchUnit covers what Modulith does not see.

**Milestone done when:** a `curl` registers an organization, logs in and reaches a protected
endpoint; and that same `curl` without the scope returns `403` with an RFC 9457 body — and no rule of
the `identity` area is still `pending` (R14.3).
