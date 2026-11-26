# Build plan · 08 · local kubernetes

> **Status:** not started — a deployment, not a module of the monolith.

The two services, their databases and Kafka in a local cluster that anybody who clones the repository
can raise with one script ([ADR-013](adr/ADR-013-local-kubernetes-kind-kustomize-strimzi.md)). The
milestone ends when the walkthrough runs against the cluster's Gateway, and keeps its promises when
a pod is killed halfway.

Docker Compose stays the everyday path. This plan adds the second one and the proof that both
stay green.

## The layout

```
deploy/k8s/
  kind-cluster.yaml   one node, pinned image, port 80 of the host mapped to the proxy's NodePort
  platform/           what a cluster administrator installs: the GatewayClass, and metrics-server
  base/
    kustomization.yaml · namespace.yaml · gateway.yaml (Gateway + HTTPRoute)
    kafka/        kafka.yaml (Kafka + KafkaNodePool) · topics.yaml (one KafkaTopic each)
    oracle/       statefulset.yaml · service.yaml
    postgres/     statefulset.yaml · service.yaml
    api/          deployment.yaml · service.yaml · configmap.yaml
    triage/       deployment.yaml · configmap.yaml · hpa.yaml
  overlays/
    local/        kustomization.yaml: image tags, replicas · resources.yaml · envoy-proxy.yaml
scripts/
  k8s-up.sh · k8s-down.sh
```

## Step 1 · The cluster script

`scripts/k8s-up.sh`, idempotent, in this order:

1. Create the kind cluster with the port mapping the Gateway needs.
2. Install Envoy Gateway, the Strimzi operator and metrics-server from manifests pinned to an exact
   version, and the `GatewayClass`.
3. Build both images and load them with `kind load docker-image`.
4. Create the `Secret` from `.env`: database credentials, JWT secret, platform client, and the model
   key if present.
5. `kubectl apply -k deploy/k8s/overlays/local`.
6. Wait for every workload to be ready.

`scripts/k8s-down.sh` deletes the cluster.

**Check:** running `k8s-up.sh` twice in a row changes nothing the second time.

## Step 2 · Kafka through Strimzi

- One `Kafka` resource with a single KRaft `KafkaNodePool`: broker and controller in one node,
  replication factor 1.
- One `KafkaTopic` per topic, partitions declared in the file:
  - `coldchain.telemetry.excursion.v1` and `coldchain.triage.alert.v1` with 3 partitions each;
  - the two dead letter topics with 1.
- The services reach the bootstrap service Strimzi creates. Nothing outside the cluster talks to
  Kafka.

**Check:** `kubectl get kafkatopics` lists the four topics, ready.

## Step 3 · The databases

- Oracle Free (`gvenzl/oracle-free:23-slim-faststart`) and PostgreSQL 17 as `StatefulSet`s, each with
  a `volumeClaimTemplate` and a headless `Service`.
- Readiness from each image's own health check. The services start against a database that answers,
  not one that merely exists.

**Check:** deleting the Oracle pod brings it back with its data. The certificate issued before is still
there.

## Step 4 · The two services

- `Deployment`s for `api` and `triage`, with `readOnlyRootFilesystem`, a non-root user and
  `allowPrivilegeEscalation: false`, the same posture as in Compose.
- **Probes from the actuator:**
  - readiness on `/actuator/health/readiness`, liveness on `/actuator/health/liveness`, and a
    `startupProbe` long enough for Flyway on a first start;
  - liveness never checks the database. A database that is down must make a pod not ready, never
    restart it.
- `requests` and `limits` on every container, and `-XX:MaxRAMPercentage` keeps the heap inside them.
- Configuration in a `ConfigMap`, credentials from the `Secret`. Nothing environment-specific is baked
  into an image.
- `server.shutdown: graceful` and a `terminationGracePeriodSeconds` longer than its timeout, so a pod
  being replaced finishes the requests and the publications it has in flight.

**Check:** `kubectl rollout restart deployment/api` during the walkthrough does not fail a single step.

## Step 5 · The Gateway

- One `Gateway` on port 80 and one `HTTPRoute`, served by Envoy Gateway. ingress-nginx was the first
  plan, and it was retired in March 2026 (ADR-013).
- The route lists the public prefixes one by one: `/api/v1`, `/api/actuator/health`,
  `/api/swagger-ui` (with the exact `/api/swagger-ui.html`) and `/api/v3/api-docs`. A single `/api`
  prefix would carry `/api/internal` with it, so the list is the control
  ([ADR-011](adr/ADR-011-platform-credential-for-service-calls.md)). Triage has no business HTTP and
  no route.
- `scripts/demo.sh` runs unchanged with `BASE_URL` pointing at the Gateway.

**Check:** `curl` on `/api/internal/v1/...` through the Gateway gets `404`.

## Step 6 · Scaling the consumer

- A `HorizontalPodAutoscaler` on triage, on CPU, with a minimum of 1 and a maximum of **3**, the
  partition count of the feed.
- The experiment that explains the cap, in the plan rather than in a slide:
  - scale triage to 4 by hand;
  - `kafka-consumer-groups --describe` shows three consumers with a partition each, and the fourth
    with none.

**Check:** with three replicas, the walkthrough's alert is produced exactly once.

## Step 7 · Failure drills

Each one is a documented command and an expected outcome:

- **Kill the API pod right after an ingestion commits.** The excursion's event waits in the outbox
  and goes out when the new pod starts (`republish-outstanding-events-on-restart`). Triage receives
  it once or twice, and the alert exists once.
- **Kill a triage pod mid-triage.** Kafka hands its partitions to the others, the inbox says
  `RECEIVED`, and the triage runs again.
- **Scale the API to two replicas.** Both may resubmit a stalled publication. Duplicates reach
  triage, and the inbox drops them.

## Step 8 · CI

- A job creates a kind cluster on the runner, runs `k8s-up.sh` without a model key, and runs
  `scripts/demo.sh` against the Gateway.
- On failure it dumps `kubectl get events` and the logs of every pod.
- If the runner's memory cannot hold Oracle and Kafka together, the fallback is explicit, not
  silent: validate the manifests with kubeconform, and say so in the README.

## Step 9 · The tests that close the milestone

The cluster has no unit tests. It has the walkthrough, run against it, and the drills of step 7 run
by hand with their outcome written down.

**Milestone done when:** on a machine with only Docker, kind and `kubectl`, `scripts/k8s-up.sh` followed
by `BASE_URL=http://localhost/api scripts/demo.sh` prints the certificate and the alert; a
`kubectl delete pod` of the API in the middle of it loses no event; and the CI job that does the same on
every push is green.
