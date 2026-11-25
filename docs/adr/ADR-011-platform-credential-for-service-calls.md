# ADR-011 — A platform credential for service-to-service calls

- **Status:** Proposed
- **Date:** 2026-11-25

## Context

Triage ([ADR-010](ADR-010-second-service-for-excursion-triage.md)) needs the context of an excursion:
the readings around it, who held the shipment and when custody last changed hands, and the frozen
thresholds. It needs that context for **every** organization's excursions, because the feed carries
all of them ([ADR-009](ADR-009-kafka-for-integration-events.md)).

Nothing in the API can grant that today, and on purpose:

- **Every token carries an organization**, and the tenancy filter applies it once, at the edge.
- **Visibility is participation** ([ADR-003](ADR-003-visibility-by-participation.md), R9.9): an
  organization sees a shipment because it takes part in it, never by membership or by role.
- **Machine credentials belong to an organization** and are narrowed to ingestion, so a compromised
  gateway cannot read shipments.

A platform consumer takes part in no shipment and belongs to no organization. Pretending it does would
bend the one rule the tenancy model rests on.

## Decision

**One platform credential, defined by configuration, valid only on `/internal/**`, and `/internal/**`
accepts nothing else.**

- **The client is configuration, not tenant data.** The API reads `coldchain.platform.client-id` and
  the **Argon2id hash** of its secret (R9.11) from the environment: a Kubernetes `Secret`, or `.env`
  under Compose. No row in `api_client`, no migration and no endpoint creates it.
- **It authenticates where every machine does**, on `POST /v1/auth/client-token`. The token it gets
  back:
  - carries **no organization**;
  - marks its subject as a platform client;
  - holds exactly **one scope, `PLATFORM_EXCURSION_READ`**;
  - lives as long as an access token, fifteen minutes.
- **`PLATFORM_EXCURSION_READ` joins the scope catalog but cannot be granted.** Neither a role nor an
  `api_client` can hold it, and the scope catalogue test proves it.
- **`/internal/**` has its own security filter chain**, ordered before the public one:
  - it accepts a platform token and rejects every other token with `403`;
  - every public route rejects a platform token with `403`.
  
  Both directions are integration tests, not conventions.
- **The internal surface stays out of sight.** It is left out of the OpenAPI document served at
  `/api/v3/api-docs`, and the Kubernetes Ingress does not route it
  ([ADR-013](ADR-013-local-kubernetes-kind-kustomize-strimzi.md)). The filter chain is the control;
  the network is the second layer.
- **Writes on behalf of the platform are not a thing.** The only internal endpoint is a read. If one
  day an internal write exists, its audit actor is the platform client, never a borrowed user.

## Consequences

- **A second filter chain to keep honest.** The two tests that cross the boundary in each direction
  are what keep a future route from landing in the wrong chain.
- **The tenancy filter learns one exception**: a principal without an organization, accepted only
  inside the internal chain. Outside it, a token without an organization is still refused.
- **The secret is a deployment secret.** Rotating it means a new hash in the environment and a
  restart of both services. Accepting two hashes at once, for a rotation without a restart, is left
  until there is a reason.
- **Triage holds a credential that reads every excursion's context.** That is exactly why the scope
  is a single read and why nothing grants it by mistake.

## Alternatives considered and rejected

- **`OrganizationKind.PLATFORM` with an ordinary `api_client` row.** The client and its secret would
  have to be seeded by a migration. [ADR-008](ADR-008-reference-data-in-flyway.md) ships reference
  data that way, and a secret is not reference data. Registration would also have to learn to refuse
  the new kind, and a platform organization would appear in every query that lists organizations.
- **Triage signing its own tokens with the API's JWT key.** Whoever holds a symmetric key can mint
  any token for any organization. Sharing it turns triage into a second identity provider with none
  of the checks.
- **Mutual TLS between the services.** It is the right answer at scale. Here it needs a certificate
  authority, issuance and rotation, more machinery than the one read it would protect.
- **A separate authorization server** (Keycloak, Spring Authorization Server). A second identity system
  next to the one the API already is, for a single client with a single scope.
- **No authentication on the internal network.** "The network is trusted" holds until the first time a
  port is exposed by mistake, and in a local cluster that is one `kubectl port-forward` away.
