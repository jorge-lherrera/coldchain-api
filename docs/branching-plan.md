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
      ├── feat/rules-naming
      ├── feat/rules-structure
      ├── feat/rules-runtime
      ├── chore/ci-and-readme
      │
      │   phase two
      ├── feat/kafka-excursion-feed
      ├── docs/triage-architecture
      ├── chore/triage-foundations
      ├── feat/triage-consumer
      ├── feat/platform-credential
      ├── feat/excursion-context
      ├── feat/triage-context
      ├── feat/triage-llm
      ├── feat/advisory
      ├── feat/triage-evaluation
      ├── feat/k8s-local
      └── chore/k8s-ci-and-readme
```

- **`main`** — only takes merges from `develop`, and only when what is inside can be shown. Every
  merge to `main` carries its tag: `v0.1.0`, `v0.2.0`, … It is created in the last step, not now.
- **`develop`** — the integration branch. No working branch comes off another working branch: they all
  come off an up-to-date `develop`.
- **`docs/*`**, **`chore/*`**, **`feat/*`** — one unit of work each. They are merged with `--no-ff` so
  the merge stays visible in the graph, and **kept** afterwards, locally and on the remote: the list of
  branches is part of the record this repository shows.

## Commit convention

`<type>(<module>): <subject>` **in English**, present tense and lowercase, no trailing period. The
code and the history are what anyone who opens the repository reads; whatever language the
documentation is written in, the commits are in English.

Types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `perf`.
Scopes: `identity`, `catalog`, `shipment`, `telemetry`, `compliance`, `advisory`, `core`, `db`, `build`,
and, outside the monolith, `triage` and `deploy`.

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

### 3 · `feat/identity` — 9 tables

Organizations, users, roles, permissions, login, machine credentials and auditing. The biggest module,
and the one that leaves ready the security the other four use.

```
feat(identity): schema for the nine module tables
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

### 8 · `feat/rules-naming` — the canon of names

The rules each module branch left `planned` are closed in three branches, one per kind of thing they
govern, because a hundred enforcers landing at once is a review nobody does.

This one is names: packages, types, members, constants, enum constants and type parameters; entities
that say in their name that they map a table; columns and tables in `UPPER_SNAKE_CASE` and singular;
constraints and indexes that say what they guarantee and on what. Writing them found the abbreviations
the schema had been carrying and the two concepts that answered to two names each, so the renames
travel in this branch too.

```
refactor(core): spell out what the schema had been abbreviating
test(core): enforce the naming canon on identifiers, columns and constraints
```

**Done when:** no name in the schema has to be guessed, and `./gradlew rules` is green.

### 9 · `feat/rules-structure` — where each thing lives

A suffix decides a folder, the contract admits only what it publishes, an aggregate is built through
a named factory, a table has exactly one owning module, and the schema belongs to Flyway alone.

```
feat(core): seal the events shipment and telemetry publish
test(core): enforce where each role lives and what the contract may say
refactor(core): one name per concept, and an id that survives a read
test(core): enforce how a class is built and who owns each table
```

**Done when:** every structural rule has a machine and the module graph is proven, not asserted.

### 10 · `feat/rules-runtime` — what only shows at run time

Optimistic locking on the aggregates that change state, observability, the contract snapshots that
make a version grow and never shrink, and the rules that need a real Oracle: delete actions, query
budgets checked by exact equality, visibility by participation, and the custody sequence under two
concurrent writers.

```
feat(core): stop the second writer from erasing the first
test(core): close the catalogue — no rule is left without a machine
docs: the documentation the five modules actually built
fix(telemetry): point every reading at the batch that actually brought it
fix(shipment): refuse to dispatch behind a device that does not exist
```

**Done when:** the catalogue has no `planned` row left and `./gradlew rebuild` is green from nothing.

### 11 · `chore/ci-and-readme`

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

### 12 · `main`

```
git checkout main && git merge --no-ff develop && git tag v1.0.0
```

## Phase two — a second service, a language model and Kubernetes

The monolith is done; phase two puts something on the other side of its Kafka feed. The decisions are
[ADR-009](adr/ADR-009-kafka-for-integration-events.md) to
[ADR-013](adr/ADR-013-local-kubernetes-kind-kustomize-strimzi.md), and the build plans are
[06 advisory](plan-06-advisory.md), [07 triage](plan-07-triage-service.md) and
[08 kubernetes](plan-08-local-kubernetes.md). The rule of phase one holds: the next branch is not opened
until the previous one is merged.

### 13 · `feat/kafka-excursion-feed` — merged

Excursions leave the monolith on `coldchain.telemetry.excursion.v1` through an outbox, in the order
they happened.

```
docs(adr): decide Kafka for outbound excursion events
fix(telemetry): keep an excursion's id when a later batch finds it again
feat(telemetry): publish excursions to Kafka through an ordered outbox
docs: excursions leave through Kafka
```

**Done when:** the walkthrough runs with Kafka in Compose and `ExcursionFeedIT` proves the key, the
order and that a rollback sends nothing.

### 14 · `docs/triage-architecture`

Phase two written down before any of it is built, the way phase one started.

```
docs(adr): a second service, a platform credential, advice not evidence, local Kubernetes
docs: build plans for advisory, triage and Kubernetes
docs: phase two in the specification and the branching plan
```

**Done when:** every link between the ADRs, the plans, the specification and the `README` resolves,
and `./gradlew rules` is still green.

### 15 · `chore/triage-foundations`

[Plan 07](plan-07-triage-service.md), step 1: the service's build, its PostgreSQL, its image and its CI job.

```
chore(triage): scaffold the service with its own build and PostgreSQL
chore(triage): production image and the service in compose
chore(build): run the triage build in CI
docs: the triage foundations as they were built
```

**Done when:** `docker compose up -d --wait` leaves five healthy containers.

### 16 · `feat/triage-consumer`

[Plan 07](plan-07-triage-service.md), step 2: the inbox, the alert in the order things happened, the rules and
the dead letter topic.

```
feat(triage): alerts from the excursion feed, in the order things happened
feat(triage): rule-based triage
feat(triage): an inbox that drops duplicates and resumes the unfinished
feat(triage): consume the feed, and dead-letter only what can never be processed
fix(build): keep every Gradle wrapper on LF line endings
test(triage): the shape rules of the service
docs: the triage consumer as it was built
```

**Done when:** a duplicate is one alert, a close before its open is a closed alert, and a malformed
message does not stop the consumer.

### 17 · `feat/platform-credential`

[Plan 07](plan-07-triage-service.md), step 3, in the API.

```
feat(identity): a platform client defined by configuration
feat(identity): an internal filter chain that only the platform token enters
docs: the platform credential as it was built
```

**Done when:** each token is refused on the other's routes, both ways, in an integration test.

### 18 · `feat/excursion-context`

[Plan 07](plan-07-triage-service.md), step 4, in the API.

```
feat(telemetry): read the readings of a window for the platform
feat(shipment): the context of an excursion for internal callers
```

**Done when:** the context of the walkthrough's excursion comes back with the carrier holding custody.

### 19 · `feat/triage-context`

[Plan 07](plan-07-triage-service.md), step 5.

```
feat(triage): ask the API for the excursion context with the platform token
feat(triage): triage without context when the API does not answer
```

**Done when:** with the API stopped, the alert still exists, with `context_available = false`.

### 20 · `feat/triage-llm`

[Plan 07](plan-07-triage-service.md), step 6.

```
feat(triage): ask the model for a probable cause through a port
feat(triage): fall back to rules when the model does not answer
```

**Done when:** the same excursion is triaged with `source = LLM` with a key and `source = RULES`
without one.

### 21 · `feat/advisory`

[Plan 07](plan-07-triage-service.md), step 7, and [plan 06](plan-06-advisory.md): the alert goes back, and the
API shows it.

```
feat(triage): publish triaged alerts through the outbox
feat(advisory): schema for the excursion alert
feat(advisory): consume triaged alerts, the newest one wins
feat(advisory): the alerts of a shipment, by participation
test(core): the verdict never reads advice
docs: six modules, and the alert in the walkthrough
```

**Done when:** `scripts/demo.sh` prints the alert, an outsider gets `404`, and `compliance` importing
`advisory` fails the build.

### 22 · `feat/triage-evaluation`

[Plan 07](plan-07-triage-service.md), step 8.

```
test(triage): golden set of excursions and the evaluation task
docs(triage): what the model gets right and what it gets wrong
```

**Done when:** `./gradlew evalTriage`, in `triage/`, prints the confusion matrix, and the triage README
publishes it.

### 23 · `feat/k8s-local`

[Plan 08](plan-08-local-kubernetes.md), steps 1 to 7.

```
chore(deploy): kind cluster script with ingress-nginx and Strimzi
chore(deploy): Kafka, Oracle and PostgreSQL in the cluster
chore(deploy): the two services with probes, limits and an autoscaler
```

**Done when:** `scripts/k8s-up.sh` and the walkthrough against the Ingress are green on a new cluster.

### 24 · `chore/k8s-ci-and-readme`

[Plan 08](plan-08-local-kubernetes.md), step 8, and the documentation that closes the phase.

```
chore(build): run the walkthrough against a kind cluster in CI
docs: running ColdChain on Kubernetes
docs(adr): accept the phase two decisions
```

**Done when:** the CI job on kind is green, the `README` explains both ways of running the system, and
ADR-010 to ADR-013 are `Accepted`.

### 25 · `main`

```
git checkout main && git merge --no-ff develop && git tag v2.0.0
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
