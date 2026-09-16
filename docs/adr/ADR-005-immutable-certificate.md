# ADR-005 — The certificate is immutable and versioned

- **Status:** Accepted
- **Date:** 2026-08-27

## Context

The compliance certificate is what the system hands to the outside world: somebody downloads it, files
it, attaches it to a claim, or produces it during an inspection. From the moment it leaves, it stops
being an internal record and becomes a document.

And yet the data it is computed from **can arrive late**. A gateway out of signal range syncs three hours
after delivery; a device is downloaded on arrival at the warehouse and dumps its entire memory. Those
readings are legitimate and they change the verdict: a shipment certified as compliant can turn out
non-compliant once the missing stretch shows up.

Late data has to be absorbable without the already delivered document changing behind the back of
whoever holds it.

## Decision

An issued certificate is **never modified**. There is no `UPDATE` on `compliance_certificate`.

- When data arrives that alters the computation, **version N+1** is issued. The previous one is not
  deleted: it becomes superseded and stays queryable.
- Uniqueness is twofold: the pair `(shipment_id, version)` is unique, and a function-based unique index
  guarantees there is only **one current version** per shipment.
- Every certificate stores its `content_hash`: whoever downloaded the PDF can verify that what they hold
  is exactly what was issued.
- Every certificate carries the list of `certificate_finding` that backs its verdict, and every finding
  points at the fact that produced it — the specific excursion, the stretch with no data, the handoff
  never accepted. **A verdict without traceable findings is not issued.**
- The quality review (`reviewed_by`, `reviewed_at`) is recorded, but it **does not change the verdict**:
  a human attests that they have looked at it, they do not correct it.

## Consequences

- The answer to "what did the certificate you handed us on Tuesday say?" is exact, and it stays exact
  after the late data arrives.
- The sequence of versions **tells a story** that turns out to be the most interesting one in the system:
  you can see that the shipment was certified as compliant and that eight hours later the stretch that
  sank it showed up. That trail is the best demonstration the project has.
- The consumer has to know that versions exist: the API returns the current one by default and offers the
  history explicitly.
- The verdict is always computed from the shipment's `threshold_snapshot`
  ([ADR-004](ADR-004-frozen-thresholds.md)), never by reading the catalog. Without that, the certificate's
  immutability would be a lie: the frozen number would depend on a mutable one.
- Issuing a new version **is not free**: the document has to be regenerated and sealed again. It is done
  only when the computation genuinely changes, not every time a reading comes in.
- The table grows with every re-issue. It is a small table — one row per closing and per correction — so
  it is accepted without further thought.

## Alternatives considered and rejected

- **Recompute the verdict on every read, without persisting it.** A verdict has to be a dated, signed
  fact, not a query that returns something different each time. It would also make `content_hash`
  impossible.
- **`UPDATE` with an audit table alongside.** The audit records that something changed, but it does not
  let you reconstruct the document that was delivered. And in practice nobody reads it.
- **Delete the previous version when issuing the new one.** It leaves the system consistent and with no
  memory: it destroys precisely the trail that makes the certificate credible.
