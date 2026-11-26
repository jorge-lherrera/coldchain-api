#!/usr/bin/env bash
#
# The whole system in a local Kubernetes cluster (ADR-013).
#
#   scripts/k8s-up.sh
#   BASE_URL=http://localhost/api scripts/demo.sh
#
# Needs Docker, kind and kubectl, and the same .env Docker Compose reads. It starts from
# nothing or from wherever the cluster was: running it again changes nothing that has not
# changed. scripts/k8s-down.sh deletes the cluster.

set -euo pipefail

CLUSTER=coldchain
NAMESPACE=coldchain
ENVOY_GATEWAY_VERSION=v1.10.0
STRIMZI_VERSION=1.3.1

cd "$(dirname "$0")/.."

step() { printf '\n\033[1m%s\033[0m\n' "$1"; }
told() { printf '   %s\n' "$1"; }
k() { kubectl --context "kind-$CLUSTER" "$@"; }

for tool in docker kind kubectl curl; do
    if ! command -v "$tool" > /dev/null 2>&1; then
        printf 'The cluster needs %s on the PATH.\n' "$tool" >&2
        exit 1
    fi
done

if [ ! -f .env ]; then
    printf 'Copy .env.example to .env first: the cluster reads its credentials from it.\n' >&2
    exit 1
fi

env_value() {
    local line
    line="$(grep -E "^$1=" .env | tail -n 1 || true)"
    line="${line#*=}"
    line="${line%$'\r'}"
    case "$line" in
        \'*\' | \"*\") line="${line:1:${#line}-2}" ;;
    esac
    printf '%s' "$line"
}

CREDENTIALS=(ORACLE_SYSTEM_PASSWORD DB_USERNAME DB_PASSWORD JWT_SECRET TRIAGE_DB_USERNAME
    TRIAGE_DB_PASSWORD PLATFORM_CLIENT_ID PLATFORM_CLIENT_SECRET PLATFORM_CLIENT_SECRET_HASH)
for key in "${CREDENTIALS[@]}"; do
    if [ -z "$(env_value "$key")" ]; then
        printf '%s is empty in .env.\n' "$key" >&2
        exit 1
    fi
done

build_tag() {
    local id
    id="$(docker image inspect --format '{{.Id}}' "$1:local")"
    id="${id#sha256:}"
    printf 'build-%s' "${id:0:12}"
}

PINNED="$(mktemp -d)"
trap 'rm -rf "$PINNED"' EXIT

step '1 · The cluster'
if kind get clusters | grep -qx "$CLUSTER"; then
    told "kind cluster $CLUSTER is already running"
else
    kind create cluster --config deploy/k8s/kind-cluster.yaml --wait 120s
fi

step '2 · Envoy Gateway, Strimzi and metrics-server, at pinned versions'
k apply -f deploy/k8s/base/namespace.yaml > /dev/null
installed="$(k get deployment envoy-gateway -n envoy-gateway-system \
    -o jsonpath='{.spec.template.spec.containers[0].image}' 2> /dev/null || true)"
if [ "$installed" = "envoyproxy/gateway:$ENVOY_GATEWAY_VERSION" ]; then
    told "Envoy Gateway $ENVOY_GATEWAY_VERSION is already installed"
else
    k apply --server-side \
        -f "https://github.com/envoyproxy/gateway/releases/download/$ENVOY_GATEWAY_VERSION/install.yaml" > /dev/null
fi
curl --fail --silent --show-error --location \
    "https://github.com/strimzi/strimzi-kafka-operator/releases/download/$STRIMZI_VERSION/strimzi-cluster-operator-$STRIMZI_VERSION.yaml" \
    | sed "s/namespace: myproject/namespace: $NAMESPACE/" \
    | k apply --server-side -n "$NAMESPACE" -f - > /dev/null
k wait --for=condition=Established --timeout=120s \
    crd/gatewayclasses.gateway.networking.k8s.io crd/gateways.gateway.networking.k8s.io \
    crd/httproutes.gateway.networking.k8s.io crd/envoyproxies.gateway.envoyproxy.io \
    crd/kafkas.kafka.strimzi.io crd/kafkanodepools.kafka.strimzi.io crd/kafkatopics.kafka.strimzi.io > /dev/null
k apply --server-side --force-conflicts -k deploy/k8s/platform > /dev/null
k rollout status -n envoy-gateway-system deployment/envoy-gateway --timeout=300s
k rollout status -n "$NAMESPACE" deployment/strimzi-cluster-operator --timeout=300s
k rollout status -n kube-system deployment/metrics-server --timeout=300s

step '3 · The two images, built here and loaded into the cluster'
docker build --provenance=false --tag coldchain-api:local .
docker build --provenance=false --tag coldchain-triage:local triage
API_TAG="$(build_tag coldchain-api)"
TRIAGE_TAG="$(build_tag coldchain-triage)"
docker tag coldchain-api:local "coldchain-api:$API_TAG"
docker tag coldchain-triage:local "coldchain-triage:$TRIAGE_TAG"
kind load docker-image --name "$CLUSTER" "coldchain-api:$API_TAG" "coldchain-triage:$TRIAGE_TAG"

step '4 · The credentials, from .env into a Secret'
literals=()
for key in "${CREDENTIALS[@]}" ANTHROPIC_API_KEY; do
    literals+=("--from-literal=$key=$(env_value "$key")")
done
k create secret generic coldchain-credentials -n "$NAMESPACE" "${literals[@]}" \
    --dry-run=client --output=yaml \
    | k apply -f -

step '5 · The manifests, with the images of this build'
k kustomize deploy/k8s/overlays/local > "$PINNED/overlay.yaml"
cat > "$PINNED/kustomization.yaml" <<EOF
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - overlay.yaml
images:
  - name: coldchain-api
    newTag: $API_TAG
  - name: coldchain-triage
    newTag: $TRIAGE_TAG
EOF
k apply --server-side --force-conflicts -k "$PINNED"

step '6 · Waiting for every workload to be ready'
k wait -n "$NAMESPACE" --for=condition=Ready --timeout=900s kafka/coldchain
k wait -n "$NAMESPACE" --for=condition=Ready --timeout=300s kafkatopic --all
k rollout status -n "$NAMESPACE" statefulset/oracle --timeout=900s
k rollout status -n "$NAMESPACE" statefulset/postgres --timeout=300s
k rollout status -n "$NAMESPACE" deployment/api --timeout=600s
k rollout status -n "$NAMESPACE" deployment/triage --timeout=600s
k wait -n "$NAMESPACE" --for=condition=Programmed --timeout=300s gateway/coldchain

step 'The cluster is up'
told 'BASE_URL=http://localhost/api scripts/demo.sh'
told 'scripts/k8s-down.sh deletes it'
