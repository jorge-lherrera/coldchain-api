#!/usr/bin/env bash
#
# The walkthrough from the README, over HTTP against a running API.
#
#   docker compose up -d --wait
#   scripts/demo.sh
#
# Three organizations, one shipment, a temperature series with an excursion and a
# blackout, and the certificate the system issues on its own when the shipment closes.
# The script fails if the verdict is not the one the README claims.

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/api}"
PASSWORD="${DEMO_PASSWORD:-demo-password-2026}"
RUN="$(date +%s)"

JOURNEY_MINUTES=180
SAMPLE_MINUTES=5
EXCURSION_FROM=60
EXCURSION_TO=100
BLACKOUT_FROM=110
BLACKOUT_TO=165
IN_BAND_CELSIUS=4.5
EXCURSION_CELSIUS=11.5

bold() { printf '\033[1m%s\033[0m\n' "$1"; }
step() { printf '\n\033[1m%s\033[0m\n' "$1"; }
told() { printf '   %s\n' "$1"; }

for tool in curl jq; do
    if ! command -v "$tool" > /dev/null 2>&1; then
        printf 'This walkthrough needs %s on the PATH.\n' "$tool" >&2
        exit 1
    fi
done

request() {
    local method="$1" path="$2" token="${3:-}" body="${4:-}"
    local arguments=(--silent --show-error --request "$method" "$BASE_URL$path"
        --header 'Content-Type: application/json')
    if [ -n "$token" ]; then
        arguments+=(--header "Authorization: Bearer $token")
    fi
    if [ -n "$body" ]; then
        arguments+=(--data "$body")
    fi

    local response status payload
    response="$(curl "${arguments[@]}" --write-out $'\n%{http_code}')"
    status="${response##*$'\n'}"
    payload="${response%$'\n'*}"
    if [ "$status" -ge 400 ]; then
        printf '\n%s %s answered %s\n%s\n' "$method" "$path" "$status" "$payload" >&2
        exit 1
    fi
    printf '%s' "$payload"
}

data() {
    request "$@" | jq -c '.data'
}

await_api() {
    local waited=0
    until curl --silent --fail "$BASE_URL/actuator/health" > /dev/null 2>&1; do
        if [ "$waited" -ge 300 ]; then
            printf 'The API never answered at %s.\n' "$BASE_URL" >&2
            exit 1
        fi
        sleep 3
        waited=$((waited + 3))
    done
}

register() {
    local slug="$1" legal_name="$2" kind="$3"
    data POST /v1/organizations '' "$(jq -cn \
        --arg taxId "$slug-$RUN" \
        --arg legalName "$legal_name" \
        --arg kind "$kind" \
        --arg email "$slug-$RUN@coldchain.dev" \
        --arg fullName "$legal_name administrator" \
        --arg password "$PASSWORD" \
        '{taxId: $taxId, legalName: $legalName, tradeName: $legalName, kind: $kind,
          country: "UY", administratorEmail: $email, administratorFullName: $fullName,
          administratorPassword: $password}')"
}

token_of() {
    data POST /v1/auth/login '' "$(jq -cn \
        --arg email "$1" --arg password "$PASSWORD" \
        '{email: $email, password: $password}')" | jq -r '.accessToken'
}

series() {
    jq -cn \
        --argjson from "$1" \
        --argjson journey "$JOURNEY_MINUTES" \
        --argjson every "$SAMPLE_MINUTES" \
        --argjson excursionFrom "$EXCURSION_FROM" \
        --argjson excursionTo "$EXCURSION_TO" \
        --argjson blackoutFrom "$BLACKOUT_FROM" \
        --argjson blackoutTo "$BLACKOUT_TO" \
        --argjson inBand "$IN_BAND_CELSIUS" \
        --argjson excursion "$EXCURSION_CELSIUS" \
        '[range(0; $journey + 1; $every)
          | select(. < $blackoutFrom or . > $blackoutTo)
          | {measuredAt: (($from + . * 60) | todate),
             celsius: (if . >= $excursionFrom and . <= $excursionTo
                       then $excursion else $inBand end)}]'
}

bold 'ColdChain — the walkthrough'
told "against $BASE_URL"
await_api

step '1 · Three organizations that do not take each other'"'"'s word for it'
laboratory="$(register lab 'Northwind Biologics' SHIPPER)"
carrier="$(register carrier 'Andes Cold Logistics' CARRIER)"
hospital="$(register hospital 'Hospital de Clinicas' WAREHOUSE)"
laboratory_token="$(token_of "$(jq -r '.email' <<< "$laboratory")")"
carrier_token="$(token_of "$(jq -r '.email' <<< "$carrier")")"
hospital_token="$(token_of "$(jq -r '.email' <<< "$hospital")")"
told "laboratory  $(jq -r '.organizationId' <<< "$laboratory")"
told "carrier     $(jq -r '.organizationId' <<< "$carrier")"
told "hospital    $(jq -r '.organizationId' <<< "$hospital")"

step '2 · The laboratory declares what it ships and under what conditions'
origin="$(data POST /v1/sites "$laboratory_token" "$(jq -cn --arg code "LAB-$RUN" \
    '{code: $code, name: "Northwind plant", kind: "ORIGIN", latitude: -34.90,
      longitude: -56.16, timeZone: "America/Montevideo"}')")"
destination="$(data POST /v1/sites "$laboratory_token" "$(jq -cn --arg code "HOSP-$RUN" \
    '{code: $code, name: "Hospital pharmacy", kind: "DESTINATION", latitude: -34.92,
      longitude: -56.17, timeZone: "America/Montevideo"}')")"
profile="$(data POST /v1/storage-profiles "$laboratory_token" "$(jq -cn --arg code "FRIDGE-$RUN" \
    '{code: $code, name: "Fridge 2-8", thresholds: {minCelsius: 2.00, maxCelsius: 8.00,
      maxSingleExcursionMinutes: 30, maxCumulativeExcursionMinutes: 120,
      minCoveragePercent: 80.00}}')")"
profile_id="$(jq -r '.id' <<< "$profile")"
request POST "/v1/storage-profiles/$profile_id/activation" "$laboratory_token" > /dev/null
product="$(data POST /v1/products "$laboratory_token" "$(jq -cn --arg sku "VAC-$RUN" \
    --arg profile "$profile_id" \
    '{sku: $sku, name: "Influenza vaccine", storageProfileId: $profile}')")"
told "profile 2 – 8 °C · 30 min single · 120 min cumulative · 80 % coverage"
told "product $(jq -r '.sku' <<< "$product")"

step '3 · A shipment of 400 vials leaves with a sensor attached'
shipment="$(data POST /v1/shipments "$laboratory_token" "$(jq -cn \
    --arg reference "SHP-$RUN" \
    --arg origin "$(jq -r '.id' <<< "$origin")" \
    --arg destination "$(jq -r '.id' <<< "$destination")" \
    --arg consignee "$(jq -r '.organizationId' <<< "$hospital")" \
    --arg product "$(jq -r '.id' <<< "$product")" \
    '{reference: $reference, originSiteId: $origin, destinationSiteId: $destination,
      consigneeOrganizationId: $consignee,
      lines: [{productId: $product, quantity: 400.000, unit: "VIAL"}]}')")"
shipment_id="$(jq -r '.id' <<< "$shipment")"
device="$(data POST /v1/devices "$laboratory_token" "$(jq -cn --arg serial "SN-$RUN" \
    --arg calibratedAt "$(jq -rn --argjson now "$RUN" '($now - 30 * 86400) | todate')" \
    '{serialNumber: $serial, model: "Tag-1", firmware: "1.4.2",
      samplingIntervalSeconds: 300, calibratedAt: $calibratedAt}')")"
device_id="$(jq -r '.id' <<< "$device")"
dispatched="$(data POST "/v1/shipments/$shipment_id/dispatch" "$laboratory_token" \
    "$(jq -cn --arg device "$device_id" '{deviceId: $device}')")"
dispatched_at="$(jq -r '.dispatchedAt' <<< "$dispatched" | sed -E 's/\.[0-9]+Z$/Z/')"
dispatched_epoch="$(jq -rn --arg moment "$dispatched_at" '$moment | fromdateiso8601')"
request POST "/v1/devices/$device_id/assignment" "$laboratory_token" "$(jq -cn \
    --arg shipment "$shipment_id" --arg attachedAt "$dispatched_at" \
    '{shipmentId: $shipment, minCelsius: 2.00, maxCelsius: 8.00, attachedAt: $attachedAt}')" \
    > /dev/null
told "$(jq -r '.reference' <<< "$dispatched") is $(jq -r '.status' <<< "$dispatched"), \
thresholds frozen at dispatch"

step '4 · Custody changes hands with a single-use code'
handoff="$(data POST "/v1/shipments/$shipment_id/handoffs" "$laboratory_token" "$(jq -cn \
    --arg to "$(jq -r '.organizationId' <<< "$carrier")" '{toOrganizationId: $to}')")"
told "code $(jq -r '.code' <<< "$handoff"), valid until $(jq -r '.expiresAt' <<< "$handoff")"
accepted="$(data POST "/v1/shipments/$shipment_id/handoffs/acceptance" "$carrier_token" \
    "$(jq -cn --arg code "$(jq -r '.code' <<< "$handoff")" '{code: $code}')")"
told "the carrier now holds it: $(jq -r '.currentCustodianOrganizationId' <<< "$accepted")"

step '5 · The gateway pushes the journey, and then pushes it again'
readings="$(series "$dispatched_epoch")"
batch="$(data POST /v1/telemetry/batches "$laboratory_token" "$(jq -cn \
    --arg device "$device_id" --arg key "demo-$RUN" --argjson readings "$readings" \
    '{deviceId: $device, idempotencyKey: $key, readings: $readings}')")"
told "$(jq -r '.receivedCount' <<< "$batch") readings sent, \
$(jq -r '.acceptedCount' <<< "$batch") accepted, \
$(jq -r '.discardedCount' <<< "$batch") discarded"
replay="$(data POST /v1/telemetry/batches "$laboratory_token" "$(jq -cn \
    --arg device "$device_id" --arg key "demo-$RUN" --argjson readings "$readings" \
    '{deviceId: $device, idempotencyKey: $key, readings: $readings}')")"
told "the same batch again: $(jq -r '.status' <<< "$replay"), nothing duplicated"

step '6 · It arrives, and the hospital takes delivery'
request POST "/v1/shipments/$shipment_id/arrival" "$carrier_token" > /dev/null
delivered="$(data POST "/v1/shipments/$shipment_id/delivery" "$hospital_token")"
told "$(jq -r '.status' <<< "$delivered") at $(jq -r '.closedAt' <<< "$delivered")"

step '7 · Nobody asked for a certificate. There is one.'
certificate="$(data GET "/v1/shipments/$shipment_id/certificate" "$laboratory_token")"
verdict="$(jq -r '.verdict' <<< "$certificate")"
told "verdict            $verdict"
told "coverage           $(jq -r '.coveragePercent' <<< "$certificate") % of the expected samples"
told "longest excursion  $(jq -r '.longestExcursionMinutes' <<< "$certificate") minutes"
told "content hash       $(jq -r '.contentHash' <<< "$certificate")"
jq -r '.findings[] | "   finding            \(.code) · \(.severity) · \(.detail)"' \
    <<< "$certificate"

expected_findings='["DATA_GAP","EXCURSION_ABOVE_MAX"]'
actual_findings="$(jq -c '[.findings[].code] | sort | unique' <<< "$certificate")"
if [ "$verdict" != "FAIL" ] || [ "$actual_findings" != "$expected_findings" ]; then
    printf '\nThe walkthrough did not end where the README says it does.\n' >&2
    printf 'expected FAIL with %s, got %s with %s\n' \
        "$expected_findings" "$verdict" "$actual_findings" >&2
    exit 1
fi

step 'The journey is on the record'
told "GET $BASE_URL/v1/shipments/$shipment_id/timeline"
told "GET $BASE_URL/v1/shipments/$shipment_id/series"
told "the whole contract at $BASE_URL/swagger-ui.html"
