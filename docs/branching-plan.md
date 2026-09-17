# Branching plan and build order

This repository is public and reads as a work sample. The history is part of what it shows: **every
branch tells one understandable unit of work**, and every merge leaves the project compiling. There
are no "assorted fixes" branches.

## The tree

```
main                                  publishable at any moment
 └── develop                          integration
      ├── docs/specification-and-adrs
      ├── chore/foundations
      ├── feat/identity
      ├── feat/catalog
      ├── feat/shipment
      ├── feat/telemetry
      ├── feat/compliance
      └── chore/ci-and-readme
```

- **`main`** — only takes merges from `develop`, and only when what is inside can be shown. Every
  merge to `main` carries its tag: `v0.1.0`, `v0.2.0`, … It is created in the last step, not now.
- **`develop`** — the integration branch. No working branch comes off another working branch: they all
  come off an up-to-date `develop`.
- **`docs/*`**, **`chore/*`**, **`feat/*`** — one unit of work each. They are merged with `--no-ff` so
  the merge stays visible in the graph, and deleted afterwards.

## Commit convention

`<type>(<module>): <subject>` **in English**, present tense and lowercase, no trailing period. The
code and the history are what anyone who opens the repository reads; whatever language the
documentation is written in, the commits are in English.

Types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `perf`.
Scopes: `identity`, `catalog`, `shipment`, `telemetry`, `compliance`, `core`, `db`, `build`.

**No `Co-Authored-By` trailer** (see [CONTRIBUTING.md](../CONTRIBUTING.md)). The body explains the
*why* when the subject is not enough; whatever is obvious in the diff is not repeated in prose.

## The order, step by step

Each row is a branch. The next one is not opened until the previous one is merged into `develop`.

Every module branch also delivers **the rule enforcers of its own area**. The catalogue in
[`rules/project-rules.md`](../rules/project-rules.md) was written before the code, so every rule starts
at `planned` / `pending`, and a module does not close while a rule of its area is still pending
(R14.3). The gate itself — `./gradlew rules` and the workflow that runs it — is built once, in step 2.

### 0 · `develop` — the starting base

It is not a separate branch: it is the first commit of the repository.

Only the skeleton that Spring Initializr generates goes in, plus the build configuration:
`build.gradle`, `settings.gradle`, the Gradle wrapper, `.gitignore`, `.gitattributes`,
`ColdChainApplication`, `application.properties` and the three Testcontainers classes.

```
chore(build): scaffold Spring Boot 4.1.1 with Java 25, Oracle and Testcontainers
```

**Done when:** `./gradlew compileJava` gives BUILD SUCCESSFUL.

### 1 · `docs/specification-and-adrs`

All the documentation, which today is unversioned: the specification of the five modules, the six
ADRs, the implementation plans and the diagrams.

```
docs: specification of the five modules, ADRs, plans and data model
docs: rule catalogue and database standard
```

**Done when:** the links between `README`, `docs/adr/README.md` and the specification resolve.

### 2 · `chore/foundations`

What step 0 of the `identity` plan calls foundations, and which today is missing entirely. It is the
longest non-module branch, because everything after it depends on the shape it fixes.

**Infrastructure:**

- `docker-compose.yml` with `gvenzl/oracle-free:23-slim-faststart` and the API itself, so a clone
  reaches a running system with one command
- a multi-stage `Dockerfile` that builds the layered jar and runs it as a non-root user
- `application.yml` per profile, with `ddl-auto: none` and Flyway enabled, pointing at the compose
- the migrations folder and the Flyway configuration, still without a single table
- the baseline `SecurityConfig`: stateless, no CSRF, every route denied except the health probe, and
  the headers of R9.15. It is what identity later extends with authentication and scopes

**The package skeleton** ([ADR-006](adr/ADR-006-modules-and-events.md)), which is what every later
branch fills in:

```
com.coldchain/
  ColdChainApplication.java
  delivery/web/        every controller, grouped by module
  modules/             the five, each with api/ and internal/
  shared/              what has no owner
```

**`shared/`**, born complete because the first module already needs all of it:

- the `ApiResponse` success envelope and its factory, and the `SuccessCode` enum that carries the
  HTTP status so no controller picks one (R8.1, R8.11)
- the RFC 9457 error model: `DomainException`, `ErrorCode`, `ErrorCategory` and the single
  `GlobalExceptionHandler` (R8.2, R8.9)
- the `SortCatalog` that makes a pageable endpoint declare its sortable fields (R5.3), with the page
  size capped by `spring.data.web.pageable.max-page-size` (R5.4)
- the **UUID v7** generator and the `UUID ↔ RAW(16)` converter, with its monotonicity test (R4.3)
- `AuditableEntity` with the four audit columns (R6.15)

**The rule gate**, because a catalogue nobody runs is a wish list:

- `RuleCatalogConsistencyTest` — the rule that protects the others
- `RuleGateCoverageTest` and `RuleWaivers` — the gate has no back door and debt does not grow
- `ModuleShapeArchTest` and `ModulithVerificationTest` — the module layout and the module model
- the `rules` and `rulesDb` Gradle tasks, and the CI workflow that runs `rules` on every push and pull
  request (R0.10, R0.11)

```
chore(db): Oracle 23ai in compose and Flyway with no ddl-auto
chore(build): production image and the full stack in compose
feat(core): UUID v7 generator and RAW(16) converter
feat(core): response envelope and RFC 9457 error model
feat(core): sort catalogue and the auditable entity base
chore(build): rule gate tasks and the CI workflow that runs them
test(core): catalogue consistency and gate coverage rules
test(core): gate coverage rules
test(core): module shape and Modulith verification
```

**Done when:** `docker compose up -d` leaves both containers healthy, `./gradlew bootRun` migrates
the empty database and finds nothing to apply, `./gradlew integrationTest` brings up Oracle in
Testcontainers, and `./gradlew rules` is green with every R0 row and the R1 shape rows moved from
`planned` to `yes`. The R1 rows about edges and table ownership stay `planned`: there is no module
code for them to look at yet, and a rule that passes because it found nothing is the thing R0.16
exists to forbid.

### 3 · `feat/identity` — 8 tables

Organizations, users, roles, permissions, login, machine credentials and auditing. The biggest module,
and the one that leaves ready the security the other four use.

```
feat(identity): schema for the eight module tables
feat(identity): organization registration with its first administrator
feat(identity): user invitation and activation
feat(identity): login with refresh token rotation
feat(identity): machine credentials for sensor gateways
feat(identity): scope-based authorization and organization context
feat(identity): RFC 7807 error responses
feat(identity): audit trail for every write
```

**Done when:** a `curl` registers, logs in and reaches a protected endpoint; and that same `curl`
without the scope returns `403` with an RFC 7807 body.

### 4 · `feat/catalog` — 3 tables

Storage profiles, products and sites. It is the small module, and the one that sets the CRUD pattern
the rest repeat.

```
feat(catalog): schema for profiles, products and sites
feat(catalog): paginated CRUD with soft retirement
feat(catalog): clone profile into the next version
```

**Done when:** a profile already used by a shipment cannot be edited, only cloned, and the attempt
returns a stable error code.

### 5 · `feat/shipment` — 5 tables

The shipment, the state machine, the two-step handoff, the chained custody log and visibility by
participation.

```
feat(shipment): schema for shipment, lines and participants
feat(shipment): state machine in the domain
feat(shipment): dispatch freezing the thresholds
feat(shipment): two-step handoff with single-use code
feat(shipment): hash-chained custody log
feat(shipment): listing filtered by participation
```

**Done when:** two organizations pass custody to each other with the code, and a third one that does
not take part gets a `404` when asking for the shipment by its identifier.

### 6 · `feat/telemetry` — 5 tables

Devices, idempotent batch ingestion, resolving the reading by assignment window and excursion
detection.

```
feat(telemetry): schema for devices, batches and partitioned readings
feat(telemetry): device assignment with function-based exclusion index
feat(telemetry): idempotent batch ingestion
feat(telemetry): excursion detection on batch close
feat(telemetry): time series aggregated by minute and hour
```

**Done when:** the same batch sent twice does not duplicate a single reading and answers with the
original result, and a reading outside every assignment window is discarded with a reason.

### 7 · `feat/compliance` — 2 tables

The verdict computation, the findings, the versioned certificate and the dashboard.

```
feat(compliance): schema for certificate and findings
feat(compliance): verdict computation over the frozen thresholds
feat(compliance): versioned reissue when late readings arrive
feat(compliance): PDF and CSV download with content hash
feat(compliance): compliance dashboard
```

**Done when:** the `README` demo runs end to end and the certificate comes out `FAIL` with the
excursion and the `DATA_GAP` as findings.

### 8 · `chore/ci-and-readme`

CI already exists from step 2, so this is what is left before publishing: the generated OpenAPI
document published as an artifact, the final `README` with the demo, and the script that seeds the
data for the walkthrough.

```
chore(build): publish the generated OpenAPI document
docs: final README with the demo walkthrough
chore: demo seeding script
```

**Done when:** the CI badge is green, every rule in the catalogue is `yes` / `green` or covered by a
live waiver, and somebody who clones the repository reaches the certificate following only the
`README`.

### 9 · `main`

```
git checkout main && git merge --no-ff develop && git tag v1.0.0
```

## Rules that are not skipped

1. **Nothing is committed without `./gradlew compileJava` green.** CI is not the safety net, it is the
   reminder.
2. **Tests are run scoped to the module being touched**, never the whole suite without `--tests`.
3. **One branch, one unit of work.** If something unrelated shows up halfway through, it goes out in
   its own branch.
4. **`develop` always compiles.** If a merge breaks it, it gets fixed before the next branch is
   opened.
5. **The documentation is updated in the same change that contradicts it.** A specification that lies
   is worse than not having one.
