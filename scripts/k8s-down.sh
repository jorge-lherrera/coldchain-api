#!/usr/bin/env bash
#
# Deletes the local cluster scripts/k8s-up.sh created, with its volumes. Docker Compose and
# its data are not touched.

set -euo pipefail

kind delete cluster --name coldchain
