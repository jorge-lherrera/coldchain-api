# Contributing to ColdChain

Thanks for taking a look. This is a small, opinionated codebase: a modular monolith on **Java 25**,
**Spring Boot 4** and **Oracle 23ai Free**, with five modules — `identity`, `catalog`, `shipment`,
`telemetry`, `compliance` — that are real boundaries and not folders.

Before writing code, read [`docs/specification.md`](docs/specification.md) and the decisions in
[`docs/adr/`](docs/adr/). The build order, branch by branch, is in
[`docs/branching-plan.md`](docs/branching-plan.md).

## The rules are not in this file

**Everything this project enforces lives in [`rules/project-rules.md`](rules/project-rules.md)**, from
the folder down to the field, each rule with its severity and the test that checks it. That catalogue is
the authority: if this file and the catalogue ever disagree, the catalogue wins (R0.1).

This file tells you how to run the project and how to commit. It does not restate rules, because a rule
written in two places drifts in one of them (R0.4).

The ones you will meet on your first day, by id:

| Id | About |
|---|---|
| R1.9 · R1.10 | Module boundaries: `api/` is the only door, `internal/` is nobody else's business |
| R2.1–R2.20 | What a type is called decides which folder it lives in |
| R3.1 · R3.2 | Constructor injection; the domain does not know the framework exists |
| R6.7 · R6.10 | Oracle: booleans as `NUMBER(1)`, conditional uniqueness as a function-based index |
| R8.1 · R8.2 | The two response shapes, success and RFC 9457 problem |
| R9.9 · R9.10 | Shipment visibility is participation, and invisible means `404`, not `403` |
| R13.1 · R13.2 | The schema is Flyway, hand-written, and never `ddl-auto` |

## Running it locally

You need **JDK 25** and **Docker** running. Everything else comes from the Gradle wrapper.

```bash
cp .env.example .env             # local credentials, never committed
docker compose up -d oracle      # Oracle 23ai Free, port 1521
./gradlew bootRun                # Flyway migrates, then the API on 8080
```

## Checks

```bash
./gradlew rules                                    # the rule gate. Runs on every push and PR
./gradlew rulesDb                                  # the rules that need a real Oracle
./gradlew compileJava                              # must be green before every commit
./gradlew test --tests "com.coldchain.<module>.*"  # unit tests of the module you touched
./gradlew integrationTest --tests "*<Class>IT"     # Testcontainers, needs Docker
```

`rules` is the gate, and it has no switch: no flag skips it and the workflow cannot carry `-x test` or
`--continue` (R0.15). If a rule gets in your way, fix it or open a waiver with your name and an expiry
date on it — what does not exist is a way to step over it without leaving a trace.

Two things about tests. They are always run **scoped to the module being touched** — never the whole
suite without `--tests`. And the integration tests bring up a real Oracle with Testcontainers, not an H2
pretending to be Oracle, so Docker has to be up or they fail before they start.

## Commit convention

Conventional Commits, in English, present tense, lowercase, no trailing period:

```
<type>(<module>): <subject>
```

Types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `perf`.
Scopes: `identity`, `catalog`, `shipment`, `telemetry`, `compliance`, `core`, `db`, `build`.

```
feat(telemetry): idempotent batch ingestion
fix(shipment): reject handoff acceptance after the code expires
```

**No `Co-Authored-By` trailer.** The body explains the *why* when the subject is not enough; whatever is
obvious in the diff is not repeated in prose.

One branch, one unit of work. Branches come off an up-to-date `develop`, are merged with `--no-ff`, and
every merge leaves the project compiling.

## Documentation is part of the change

If a change contradicts the specification, an ADR or a rule, **updating it is part of that change**
(R14.1). A rule that lies is worse than no rule at all.

An ADR is never edited to change your mind: you write a new one that supersedes it, and the old one
stays where it is, marked as superseded. The reasoning that was ruled out is worth as much as the
reasoning that won.
