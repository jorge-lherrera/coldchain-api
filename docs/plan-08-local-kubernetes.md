# Build plan · 08 · local kubernetes

> **Status:** closed — a deployment, not a module of the monolith. Steps 1 to 7 were built in step 23
> of the [branching plan](branching-plan.md), and step 8 in step 24.

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

What makes the second run a no-op, as built:

- **Every image is tagged with its own build**, `build-<the first twelve characters of the image
  id>`, and the overlay is applied with those tags. An unchanged build keeps its tag, so nothing
  rolls; a changed one rolls exactly its own `Deployment`. The images are built with
  `--provenance=false`: the attestation Docker adds by default carries a timestamp, and the image id
  would change on every build.
- **Server-side apply with `--force-conflicts`**: what the files declare wins over a hand edit, such
  as a `kubectl scale` left from a drill.
- **Envoy Gateway is installed only when it is not there at the pinned version.** Its manifest is
  rendered from a Helm chart and carries the certificate job as a plain `Job` that deletes itself
  thirty seconds after it ends, so applying it again would recreate the job every time.

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

- Oracle Free and PostgreSQL 17 as `StatefulSet`s, each with a `volumeClaimTemplate` and a headless
  `Service`.
- Oracle runs `gvenzl/oracle-free:23-slim`, not the `faststart` image Compose uses. The `faststart`
  image ships the database inside the image, under the path the volume mounts over: Docker copies
  it into an empty named volume, a persistent volume claim does not, and the database is never
  there. The plain image creates it in the empty volume on the first start.
- Readiness from each image's own health check. The services start against a database that answers,
  not one that merely exists: the API waits in an init container until a `select 1 from dual` comes
  back, and triage until `pg_isready` does.

**Check:** deleting the Oracle pod brings it back with its data. The certificate issued before is still
there.

## Step 4 · The two services

- `Deployment`s for `api` and `triage`, with `readOnlyRootFilesystem`, a non-root user and
  `allowPrivilegeEscalation: false`, the same posture as in Compose.
- **Probes from the actuator:**
  - readiness on `/actuator/health/readiness`, liveness on `/actuator/health/liveness`, and a
    `startupProbe` long enough for Flyway on a first start;
  - liveness never checks the database. A database that is down must make a pod not ready, never
    restart it. Spring Boot's readiness group holds only the application's own state by default,
    so both services add the database to it: `management.endpoint.health.group.readiness.include:
    readinessState,db`.
- `requests` and `limits` on every container, and `-XX:MaxRAMPercentage` keeps the heap inside them.
- Configuration in a `ConfigMap`, credentials from the `Secret`. Nothing environment-specific is baked
  into an image.
- `server.shutdown: graceful` and a `terminationGracePeriodSeconds` longer than its timeout, so a pod
  being replaced finishes the requests and the publications it has in flight. Both are explicit in
  the configuration, 30 seconds per phase against 45, and the API waits ten seconds in a `preStop`
  hook first, while the Gateway stops sending it requests.

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

As built, the job `the walkthrough on kind` in `.github/workflows/ci.yml`:

- installs kind and `kubectl` at pinned versions, each checked against its published checksum;
- copies `.env.example`, the file a new clone starts from, so the credentials are the documented
  ones;
- runs `k8s-up.sh` twice and fails if any workload, Kafka resource, route or autoscaler changed
  generation or identity on the second run, which turns the check of step 1 into a gate;
- walks `scripts/demo.sh` through the Gateway, and fails unless `/api/internal` answers `404` there;
- deletes the cluster whatever happened.

The standard runner holds the whole cluster, so the kubeconform fallback was not needed.

## Step 9 · The tests that close the milestone

The cluster has no unit tests. It has the walkthrough, run against it, and the drills of step 7 run
by hand with their outcome written down.

**Milestone done when:** on a machine with only Docker, kind and `kubectl`, `scripts/k8s-up.sh` followed
by `BASE_URL=http://localhost/api scripts/demo.sh` prints the certificate and the alert; a
`kubectl delete pod` of the API in the middle of it loses no event; and the CI job that does the same on
every push is green.

## What the cluster showed

Run by hand on 2026-11-26, on a new cluster, with no model key.

**How the drills cut traffic.** A rule at the kind node drops the packets between two pods, and
deleting it gives the route back:

```
docker exec coldchain-control-plane iptables -I FORWARD 1 -s <pod IP> -d <pod IP> -j DROP
docker exec coldchain-control-plane iptables -D FORWARD -s <pod IP> -d <pod IP> -j DROP
```

A `NetworkPolicy` was the first idea, and it did not hold: in two attempts triage's HTTP client
still reached the API while a new `curl` from the same pod timed out. The rule at the node drops
every packet, whatever the connection.

| Step | What was done | What happened |
|---|---|---|
| 1 | `k8s-up.sh` on a running cluster | No object changed generation, identity or replica count. |
| 2 | `kubectl get kafkatopics` | The four topics, ready, with 3, 3, 1 and 1 partitions. |
| 3 | `kubectl delete pod oracle-0` after a walkthrough | The certificate came back with the same content hash. Meanwhile the API went not ready and was never restarted. |
| 4 | `kubectl rollout restart deployment/api`, with walkthroughs back to back | Eleven walkthroughs in 150 seconds across the restart, all green. |
| 5 | `curl` on `/api/internal/v1/...` through the Gateway | `404`. `/api/actuator/metrics` is `404` too: it is not on the list. |
| 6 | Triage scaled to 4 | Three consumers with one partition each, and the fourth with none. |
| 6 | A walkthrough with triage at 3 replicas | Two feed events and two alerts for the shipment, as with one replica. The API keeps one row per excursion. |

The drills of step 7:

- **The API killed with the excursion in its outbox.**
  - The API's packets to Kafka were dropped before a walkthrough. After the ingestion, the outbox
    held both events of the excursion, unpublished.
  - The pod was killed with `--grace-period=0 --force`, and the route given back.
  - The new pod published both at start, once each. Triage processed each event once, and the API
    keeps one alert.
  - The walkthrough got four `503` from the Gateway while the only API pod was replaced, kept
    asking, and ended green.
- **Triage killed mid-triage.**
  - Triage was stopped during a walkthrough, then started with its packets to the API dropped.
  - The pod was killed when the inbox said `ExcursionClosed RECEIVED`, and the inbox still said it
    after the container exited.
  - With the route back, the replacement pod received the event again, logged `RESUMED` and
    finished the triage with the context. The walkthrough ended green.
- **The API at two replicas, both cut off from Kafka.**
  - Both events of the walkthrough's excursion were `FAILED` after 94 seconds.
  - Six seconds after the route came back, the outbox was empty. This time only one resubmission
    went out, and there was no duplicate to drop.
  - So the duplicate was sent by hand: the delivered `ExcursionClosed` message, again, with
    `kafka-console-producer`. Triage logged `DUPLICATE`, published no alert, and the inbox did not
    change.
