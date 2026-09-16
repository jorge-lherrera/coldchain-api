# ADR-004 — Thresholds are frozen into the shipment on dispatch

- **Status:** Accepted
- **Date:** 2026-08-27

## Context

A storage profile says at what temperature a product has to travel: the range in degrees, the minutes
tolerated out of band, and the minimum data coverage. It lives in the catalog, and the catalog changes —
a regulatory change, a reformulated product, a data-entry mistake somebody fixes six months later.

A shipment's certificate is computed by comparing its temperature series against those thresholds. If the
computation reads the profile in force at query time, **fixing a profile rewrites the verdict of shipments
that were already delivered**: a shipment certified as compliant shows up as failed, or the other way
round, without anyone having touched that shipment. It is the kind of failure that destroys trust in the
whole system, and it raises no error at all: the number just changes.

## Decision

At the moment the shipment is **dispatched**, its thresholds are copied onto the shipment row itself:
`min_celsius`, `max_celsius`, `max_cumulative_excursion_min`, `max_single_excursion_min` and
`min_coverage_pct`.

- From that instant on, the shipment never looks at the catalog again. All excursion computation and the
  entire verdict use the copy.
- Every shipment line also stores its `storage_profile_id` and its `storage_profile_version`, so the
  threshold can be traced back to where it came from.
- When several lines carry different profiles, **the strictest** of them is the one frozen.
- An `ACTIVE` profile already used by some shipment is not edited: it is cloned into version N+1 and the
  previous one moves to `RETIRED`.

## Consequences

- **History is stable.** A certificate issued today says the same thing two years from now, even if the
  catalog has changed three times in between.
- Information is duplicated on purpose. This is the conscious exception to not repeating data: here the
  copy *is* the data — the threshold in force when the box left — and not a cache of the other one.
- The catalog can evolve without fear, which is what makes it usable: nobody has to wonder which
  shipments they are about to alter by fixing a profile.
- The `compliance` module **does not need to read `catalog`** in order to certify. One edge fewer between
  modules, and the certificate is computed from what it has right in front of it.
- A shipment in draft **does** see catalog changes, because nothing has been frozen yet. Dispatch is the
  boundary, and the UI has to say so.
- Changing a threshold on an already dispatched shipment is impossible by design. If it were genuinely
  needed — a threshold captured wrong before departure — the way out is to void the shipment and create
  another, not to edit.
- The thresholds also appear in the certificate's `threshold_snapshot`, in `JSON` form, so the delivered
  document is readable without joining tables.

## Alternatives considered and rejected

- **Read the profile in force at certification time.** It is the simplest thing to write and it rewrites
  history in silence. Rejected without discussion.
- **Store only `storage_profile_id` + `version` and join at certification time.** Correct in theory: it
  forces keeping every version forever and makes `compliance` cross the `catalog` boundary on every
  computation. More coupling for less guarantee.
- **Freeze when the shipment is created rather than when it is dispatched.** It leaves the draft tied to a
  possibly stale threshold for days. Dispatch is the instant the shipment becomes a fact.
