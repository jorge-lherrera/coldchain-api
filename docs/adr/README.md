# Architecture decisions

Every decision that is expensive to reverse lives here, with its context and with what was rejected. An
ADR is **not edited when you change your mind**: you write a new one that supersedes it, and the old one
becomes `Superseded by ADR-0XX` without being deleted. The value is in the trail, not in the final
snapshot.

| # | Decision | Status |
|---|---|---|
| [001](ADR-001-oracle-23ai.md) | Oracle 23ai Free as the only database, schema owned by Flyway alone | Accepted |
| [002](ADR-002-uuid-v7-raw16.md) | UUID v7 in `RAW(16)` as the primary key | Accepted |
| [003](ADR-003-visibility-by-participation.md) | Visibility between organizations is participation in the shipment | Accepted |
| [004](ADR-004-frozen-thresholds.md) | Thresholds are frozen into the shipment on dispatch | Accepted |
| [005](ADR-005-immutable-certificate.md) | The certificate is immutable and versioned | Accepted |
| [006](ADR-006-modules-and-events.md) | Modular monolith of five modules with synchronous events | Accepted |
| [007](ADR-007-hexagonal-module-internals.md) | Hexagonal internals: pure domain, ports and adapters | Accepted |
| [008](ADR-008-reference-data-in-flyway.md) | Reference data ships in Flyway, never in an application seeder | Accepted |

An ADR explains *why*. What the project **enforces** is declared in
[`rules/project-rules.md`](../../rules/project-rules.md), and a rule there cites the ADR rather than
repeating its argument (R0.1, R0.4).

## Template

```markdown
# ADR-0XX — One-line title

- **Status:** Proposed | Accepted | Superseded by ADR-0YY
- **Date:** YYYY-MM-DD

## Context

What situation forces a decision. Facts, not opinions.

## Decision

What gets done, in the present tense and stated affirmatively.

## Consequences

What this forces us to do and what it stops us doing. Include the uncomfortable parts.

## Alternatives considered and rejected

What was considered and why not. Without this, an ADR is a press release.
```
