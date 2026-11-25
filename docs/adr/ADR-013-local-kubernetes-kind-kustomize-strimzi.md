# ADR-013 — Local Kubernetes with kind, Kustomize and Strimzi

- **Status:** Proposed
- **Date:** 2026-11-25

## Context

Docker Compose runs the whole system on one host with one command, and it stays: it is the fastest
way to develop and the way CI runs the walkthrough. What it cannot answer are the questions that
start the moment there are two services:

- What tells the platform that an instance is alive but not ready?
- What happens to an excursion when the pod that committed it dies mid-publication?
- How far can the consumer scale, and what does the next replica do?
- How does a new version replace the old one without dropping traffic?

A portfolio also has a constraint a company does not: **anybody who clones it must be able to run
it**, without an account and without a bill. A managed cluster costs money every hour it exists.

## Decision

**A local cluster with kind, manifests composed with Kustomize, and Kafka run by the Strimzi
operator.**

- **The cluster is kind**, created and torn down by `scripts/k8s-up.sh` and `scripts/k8s-down.sh`.
- **Manifests live in `deploy/k8s/`**: a `base/` and an `overlays/local/` that sets replicas,
  resources and image tags.
- **Kafka runs through Strimzi.**
  - A `Kafka` resource with one KRaft `KafkaNodePool`, and one `KafkaTopic` per topic. The topics
    become declared resources: their partition count is reviewed in a diff, not typed into a CLI.
  - The API's `NewTopic` beans stay for Compose and the tests. They only create what is missing.
- **Oracle and PostgreSQL run as `StatefulSet`s** with a persistent volume each.
- **The API and triage run as `Deployment`s:**
  - readiness and liveness come from the actuator probes the API already exposes;
  - every container declares `requests` and `limits`;
  - configuration comes from a `ConfigMap`;
  - credentials come from a `Secret` that `k8s-up.sh` creates from `.env`. The `Secret` is never a
    file in the repository.
- **The Ingress (ingress-nginx) routes the API's public prefixes, listed one by one**: `/api/v1`,
  the health probe and the OpenAPI pages. A single `/api` prefix would carry `/api/internal` with it,
  and `/internal/**` must not be reachable from outside the cluster
  ([ADR-011](ADR-011-platform-credential-for-service-calls.md)). Triage has no route at all.
- **Triage scales with a `HorizontalPodAutoscaler` capped at three replicas**, the partition count of
  its topic. A fourth consumer in the group would receive no partition and sit idle.
- **Images are built locally and loaded with `kind load`.** There is no registry to push to.
- **CI proves the manifests.** A job creates a kind cluster, applies the overlay and runs
  `scripts/demo.sh` against the Ingress.

## Consequences

- **The cluster is heavy for a laptop.** Oracle Free alone needs about 2 GB; the whole system wants
  roughly 6 GB for the cluster. Compose remains the everyday path, and the README says so.
- **Two ways to run it, kept honest by CI.** Compose and Kubernetes both run the same walkthrough on
  every push, so neither rots while the other is used.
- **Operator versions are pinned.** Strimzi's install manifest is referenced at an exact version, and
  upgrading it is a reviewed change.
- **Production concerns are documented, not demonstrated:** replication across brokers, backups,
  network policies, a secrets manager and multi-node scheduling. A single-node cluster cannot show
  them, and pretending otherwise would be the decoration this project avoids.

## Alternatives considered and rejected

- **Helm.** Templating pays off across many environments and many consumers of a chart. With one
  environment it is indirection: Kustomize ships inside `kubectl` and patches plain YAML.
- **minikube or k3d.** Equivalent for this purpose. kind is chosen because it runs the same way
  inside a GitHub Actions runner, which is where the manifests are proven.
- **Kafka as a hand-written `StatefulSet`.** It works, and it means owning the KRaft configuration,
  the listeners and topic management by hand. Strimzi is how Kafka is run on Kubernetes in practice,
  and its `KafkaTopic` is the declared topic this ADR wants.
- **A managed cluster (EKS, GKE, AKS).** Real, and billed by the hour, with an account nobody who
  clones the repository has.
- **Compose only.** It is kept, and it cannot answer the questions in the context, which are the
  reason for this decision.
