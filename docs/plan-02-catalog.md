# Build plan · 02 · catalog

> **Status:** closed

Three tables and no surprises: this is the small module, and its value is nailing down the shape the
other three repeat. **A reader who understands `catalog` understands the rest of the codebase**, so
this is the plan that spells the shape out in full. The milestone ends when a profile already used by
a shipment refuses to be edited.

It depends on `identity` (every request arrives with its organization already resolved). It depends on
nobody else. The module layout is [ADR-007](adr/ADR-007-hexagonal-module-internals.md).

## Step 1 · Migration V2 — the three tables

- `storage_profile`, `product` and `site` with `RAW(16)` PK and the four audit columns (R6.15).
- `organization_id` in all three: a column holding the UUID, **with no foreign key** — the
  organization lives in another module and you do not cross that border with a `REFERENCES`. The one
  exception is tenancy itself, which does carry one (R9.6).
- `CHECK (min_celsius < max_celsius)` and
  `CHECK (max_single_excursion_min <= max_cumulative_excursion_min)`. These are table invariants, not
  service validation: two concurrent requests can dodge an `if`, they cannot dodge a `CHECK` (R6.11).
- `CHECK (min_coverage_pct BETWEEN 0 AND 100)` and `CHECK` on `status` and `kind` — closed sets that
  change with a deployment, so they are checks and not catalogue tables (R6.13).
- `CHECK (TRIM(col) IS NOT NULL)` on every mandatory text column, because in Oracle the empty string
  **is** `NULL` and a `NOT NULL` on its own is not enough (R6.12).
- Uniqueness of `code` and `sku` **within the organization and only among the live rows**. Oracle has
  no partial indexes, so it is a function-based unique index (R6.10):

  ```sql
  CREATE UNIQUE INDEX UQ_PRODUCT_SKU_LIVE
    ON PRODUCT (CASE WHEN DELETED_AT IS NULL THEN ORGANIZATION_ID END,
                CASE WHEN DELETED_AT IS NULL THEN UPPER(SKU) END);
  ```

  Retired rows end up with both expressions at `NULL` and Oracle does not index `NULL`s: they fall out
  of the index on their own, and the SKU can be reused.
- An `organization_id` index on all three tables: they are always queried by tenancy (R9.7).

**Check:** `./gradlew bootRun` against the empty database; then twice, to confirm it does not reapply. An
`INSERT` that violates each `CHECK`, confirming the database rejects it.

## Step 2 · Domain and persistence — the four artefacts

This is the shape. Every aggregate in every module is built exactly this way, and `StorageProfile` is
the worked example:

1. **`StorageProfile`** in `internal/domain/model` — **pure Java**. No `@Entity`, no Spring, no
   annotations (R3.2). It exposes the range and the tolerances as a `Thresholds` value object rather
   than five loose fields, because that object is what later gets copied wholesale into a shipment.
   Created through `StorageProfile.createDraft(...)`, never a public no-args constructor (R3.3).
2. **`StorageProfileJpaEntity`** in `internal/infrastructure/persistence/entity` — the column mapping
   and nothing else. Extends `AuditableEntity` (R3.5).
3. **`StorageProfilePersistenceMapper`** in `.../persistence/mapper` — domain ↔ entity, with a
   round-trip unit test: domain → entity → domain must be an identity. That test is the one that
   catches the field somebody forgot to map.
4. **`StorageProfileRepositoryAdapter`** in `.../persistence/adapter` — implements the port, using a
   Spring Data `StorageProfileJpaRepository` from `.../persistence/jpa`.

The port, **`StorageProfileRepository`**, is an interface in `internal/domain/repository` that speaks
in domain models (R2.14). **No use case ever sees a JPA type** — if a `JpaEntity` shows up in an
`application/` import, the design has leaked.

The adapter is also where a database failure becomes a domain error: a `DataIntegrityViolationException`
carrying `UQ_PRODUCT_SKU_LIVE` is translated into
`DomainException.of(CatalogErrorCode.SKU_ALREADY_IN_USE)`. That translation is why a race between two
requests comes out as a `422` with a stable code instead of a `500`.

Every listing is `Pageable` and comes down to `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY` (R7.5); no
query returns an unbounded `List` (R5.4). The organization filter is not hand-written into every
query: it is applied from the context `identity` left at the edge.

## Step 3 · Use cases

Each one is a class with the `UseCase` suffix, annotated `@UseCase` and never `@Service` (R3.10),
explicit constructor injection, one public `execute(<X>Command)` returning an `<X>Result`. Commands,
results and filters are `record`s in `api/dto` (R3.8). Each ships with its unit test against fake
repositories.

**Commands** — `internal/application/usecase/command/`:

1. `CreateStorageProfileUseCase` — born in `DRAFT`, version 1.
2. `ActivateProfileUseCase` — `DRAFT → ACTIVE`. From here on a shipment can use it.
3. `CloneProfileToNextVersionUseCase` — copies the profile to version N+1 in `DRAFT` and leaves the
   previous one in `RETIRED`. **It is the only way to change the thresholds of a profile already in
   use** ([ADR-004](adr/ADR-004-frozen-thresholds.md)).
4. `UpdateProfileUseCase` — only while it is still in `DRAFT`. If it is `ACTIVE`, it throws
   `DomainException.of(CatalogErrorCode.PROFILE_IN_USE_CLONE_INSTEAD)`, which reaches the client as a
   `422` through `ErrorCategory.BUSINESS_RULE` (R8.7) — never a 500, and never a status the module
   picked by hand.
5. `CreateProductUseCase` / `UpdateProductUseCase` / `RetireProductUseCase` — retiring is a soft
   delete, it writes `deleted_at` (R6.9).
6. `CreateSiteUseCase` / `UpdateSiteUseCase` — with coordinates and its own time zone.

**Queries** — `internal/application/usecase/query/`:

7. `ListProfilesUseCase` / `ListProductsUseCase` / `ListSitesUseCase` — paginated, searchable by name
   or code, and each declaring its sortable fields (R5.3). A query never writes (R5.2).

`CatalogFacade` in `internal/application` implements `CatalogApi`, which is the only thing `shipment`
may import when it needs a profile to freeze (R1.10).

## Step 4 · Presentation and errors

- Controllers live in `delivery/web/catalog`, **outside the module** (R1.16), depend only on
  `CatalogApi` (R5.5), and carry their own `<X>Request` / `<X>Response` records plus a
  `CatalogWebMapper`. No entity and no domain model crosses to a controller (R5.6).
- `@Valid` on every request body (R5.7): shape is validated at the edge, meaning in the use case
  (R5.8).
- Success through `responseFactory.ok(SuccessCode.PROFILE_CREATED, ...)` — the controller neither
  assembles JSON nor chooses a status (R8.10, R8.11).
- Errors through the single `GlobalExceptionHandler` in `shared` (R8.9). There is no
  `@RestControllerAdvice` per module.
- The site's time zone travels as an IANA identifier (`America/Sao_Paulo`), not as an offset: the
  offset changes twice a year and the identifier does not.

## Step 5 · Tests that close the milestone

- **Unit:** the use cases, the three persistence-mapper round trips, and in particular that
  `UpdateProfileUseCase` on an `ACTIVE` profile fails and that `CloneProfileToNextVersionUseCase`
  leaves exactly one `ACTIVE` and one `RETIRED`.
- **Integration (Testcontainers):**
  - creating two products with the same SKU fails; retiring the first and creating it again works
    (this is where the function-based index gets proven, the one that would not exist in H2);
  - the range `CHECK`s reject `min > max` from the database, not from Java;
  - the listing returns only the rows belonging to the token's organization.
- **Architecture:** `catalog` declares `@ApplicationModule(allowedDependencies = {"shared",
  "identity::api"})` and Modulith verifies it (R1.5); ArchUnit adds that no `Resource` touches a
  repository directly and that no `JpaEntity` escapes `internal/infrastructure`.

**Milestone done when:** a `curl` creates a profile, activates it, tries to edit it and gets back a
`422` carrying `PROFILE_IN_USE_CLONE_INSTEAD`; clones it, and version 2 is born in `DRAFT` with the
previous one in `RETIRED` — and no rule of the `catalog` area is still `pending` (R14.3).
