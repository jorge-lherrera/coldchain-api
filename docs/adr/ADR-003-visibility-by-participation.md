# ADR-003 — Visibility between organizations is participation in the shipment

- **Status:** Accepted
- **Date:** 2026-08-27

## Context

Four different organizations may need to see the same shipment: the laboratory that ships it, one or more
carriers, the intermediate warehouse and the hospital that receives it. None of them owns the others, and
none of them should see the shipments it does not take part in.

This is the central problem of the system. Every query over shipments, readings or certificates depends
on how it is answered, and answering it badly is paid for in data leaks between customers.

I have seen two ways of solving it in production. The first, a parent/child **organization tree**:
visibility is inherited upwards or downwards depending on who is asking. It ends up implicit — nobody
knows why an organization sees a row without walking the tree by hand — and every query drags along
hierarchy clauses that nobody dares touch. The second, a model of **programs or workspaces** where
organizations join a shared space: it is correct and explicit, but it is an entire subsystem, with its
membership, its active context and its per-entity sharing rules.

## Decision

The unit of visibility is the **shipment**, and the list of who can see it is a table:
`shipment_participant` (shipment, organization, participation type).

- If your organization is not listed as a participant of a shipment, **that shipment does not exist for
  you**: not in the listing, not by direct identifier, and not through its readings or its certificate.
- There is no inheritance, no organization hierarchy, and no active context to select.
- The participation types are `SHIPPER`, `CARRIER`, `CONSIGNEE` and `OBSERVER`. `OBSERVER` is read-only
  and exists for the control laboratory or the auditor.
- The filter is applied **in the repository**, inside the same paginated query, never by filtering in
  memory after the rows have been fetched.

## Consequences

- The question "why does this organization see this shipment?" is answered by **one row**, and it can be
  shown in the UI and in the audit trail. Visibility is a fact, not a deduction.
- Gaining access is an explicit operation: add a participant. Losing it too: `revoked_at`. Who added it
  and when stays on the record.
- Every shipment query carries the join with `shipment_participant`. It is repetitive, so it lives in a
  single place in the data-access layer and is tested once, properly, with two organizations.
- **Telemetry and certificates inherit the rule** instead of having their own: the shipment is resolved
  first, and whoever cannot see it cannot see its readings either. One rule, not three.
- There is no way to say "this organization sees everything that one has". If it is ever needed — a
  holding company grouping its subsidiaries — a new concept will have to be added, and it will be a new
  ADR, not a patch on this one.
- The first integration test in the `shipment` module is the leak test: two organizations, one shipment,
  and the second one does not see it until it accepts the handoff.

## Alternatives considered and rejected

- **Parent/child organization tree.** Rejected from direct experience: visibility turns implicit, queries
  fill up with hierarchy clauses, and ripping it out later costs months.
- **Shared programs or workspaces.** It is the right model when there are many entities to share with
  different rules per entity. Here there is only one entity to share — the shipment — so the subsystem is
  dead weight: complexity with nothing to pay for it.
- **A `visible_to` field holding a list of organizations on the shipment row itself.** Impossible to
  index and impossible to audit; and in Oracle it would also force a text or `JSON` column into the worst
  possible place: a filtering predicate.
