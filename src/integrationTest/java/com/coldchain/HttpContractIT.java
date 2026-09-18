package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HttpContractIT {

    private static final String CONTEXT_PATH = "/api";

    private static final String PASSWORD = "integration-password";

    private static final Map<String, Object> FRIDGE = Map.of(
            "minCelsius", new BigDecimal("2.00"),
            "maxCelsius", new BigDecimal("8.00"),
            "maxSingleExcursionMinutes", 30,
            "maxCumulativeExcursionMinutes", 120,
            "minCoveragePercent", new BigDecimal("80.00"));

    @Autowired
    private ObjectMapper json;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping routes;

    @LocalServerPort
    private int port;

    private final Set<String> answered = new TreeSet<>();

    private RestClient http;

    private String laboratory;

    private String carrier;

    private String hospital;

    private UUID laboratoryId;

    private UUID carrierId;

    private UUID hospitalId;

    private UUID warehouseId;

    private UUID activeProfileId;

    private UUID productId;

    private UUID retiredProductId;

    private UUID originSiteId;

    private UUID destinationSiteId;

    private UUID deviceId;

    private UUID shipmentId;

    private Instant dispatchedAt;

    @Test
    @Order(1)
    void identityLetsTheThreeOrganizationsIn() {
        Answer laboratoryRegistration = register("lab", "Northwind Biologics", "SHIPPER");
        Answer carrierRegistration = register("carrier", "Andes Cold Logistics", "CARRIER");
        Answer hospitalRegistration = register("hospital", "Hospital de Clinicas", "WAREHOUSE");
        Answer warehouseRegistration = register("hub", "Puerto Cold Hub", "WAREHOUSE");

        expect(laboratoryRegistration, 201);
        laboratoryId = uuidAt(laboratoryRegistration, "organizationId");
        carrierId = uuidAt(carrierRegistration, "organizationId");
        hospitalId = uuidAt(hospitalRegistration, "organizationId");
        warehouseId = uuidAt(warehouseRegistration, "organizationId");

        laboratory = accessTokenOf(laboratoryRegistration);
        carrier = accessTokenOf(carrierRegistration);
        hospital = accessTokenOf(hospitalRegistration);

        Answer scopes = call(HttpMethod.GET, "/v1/auth/scopes", laboratory, null);
        expect(scopes, 200);
        assertThat(data(scopes).get("scopes").size())
                .describedAs("an administrator that reaches the endpoint with no scopes attached "
                        + "would pass every call below for the wrong reason")
                .isEqualTo(17);
    }

    @Test
    @Order(2)
    void aUserIsInvited_activated_promotedAndDemoted() {
        Answer invitation = call(HttpMethod.POST, "/v1/users", laboratory, Map.of(
                "email", "reviewer-" + UUID.randomUUID() + "@coldchain.dev",
                "fullName", "Quality reviewer",
                "role", "VIEWER"));
        expect(invitation, 201);
        UUID invitedId = uuidAt(invitation, "userId");

        Answer activation = call(HttpMethod.POST, "/v1/auth/activation", null, Map.of(
                "activationToken", data(invitation).get("activationToken").asString(),
                "password", PASSWORD));
        expect(activation, 200);

        Answer promotion = call(HttpMethod.POST, "/v1/users/{userId}/roles", laboratory,
                Map.of("role", "DISPATCHER"), invitedId);
        expect(promotion, 201);

        Answer demotion = call(HttpMethod.DELETE, "/v1/users/{userId}/roles/{role}", laboratory,
                null, invitedId, "DISPATCHER");
        expect(demotion, 200);

        Answer everybody = call(HttpMethod.GET, "/v1/users", laboratory, null);
        expect(everybody, 200);
        assertThat(data(everybody).size()).isEqualTo(2);
    }

    @Test
    @Order(3)
    void aMachineGetsItsOwnCredentials() {
        Answer creation = call(HttpMethod.POST, "/v1/organizations/api-clients", laboratory, Map.of(
                "label", "Gateway",
                "scopes", List.of("TELEMETRY_INGEST")));
        expect(creation, 201);

        Answer token = call(HttpMethod.POST, "/v1/auth/client-token", null, Map.of(
                "clientId", data(creation).get("clientId").asString(),
                "clientSecret", data(creation).get("clientSecret").asString()));

        expect(token, 201);
        assertThat(data(token).get("accessToken").asString())
                .describedAs("a client that cannot get a token cannot push a reading")
                .isNotBlank();
    }

    @Test
    @Order(4)
    void theCatalogueIsDeclared_amended_activatedAndVersioned() {
        Answer origin = call(HttpMethod.POST, "/v1/sites", laboratory, site("LAB", "ORIGIN"));
        Answer destination = call(HttpMethod.POST, "/v1/sites", laboratory,
                site("HOSP", "DESTINATION"));
        expect(origin, 201);
        originSiteId = uuidAt(origin, "id");
        destinationSiteId = uuidAt(destination, "id");

        Answer renamed = call(HttpMethod.PUT, "/v1/sites/{siteId}", laboratory, Map.of(
                "name", "Northwind plant, dock 2",
                "latitude", new BigDecimal("-34.90"),
                "longitude", new BigDecimal("-56.16"),
                "timeZone", "America/Montevideo"), originSiteId);
        expect(renamed, 200);
        assertThat(data(renamed).get("name").asString()).isEqualTo("Northwind plant, dock 2");

        Answer sites = call(HttpMethod.GET, "/v1/sites", laboratory, null);
        expect(sites, 200);
        assertThat(data(sites).size()).isEqualTo(2);

        Answer profile = call(HttpMethod.POST, "/v1/storage-profiles", laboratory, Map.of(
                "code", "FRIDGE-" + suffix(), "name", "Fridge 2-8", "thresholds", FRIDGE));
        expect(profile, 201);
        activeProfileId = uuidAt(profile, "id");

        Answer amended = call(HttpMethod.PUT, "/v1/storage-profiles/{profileId}", laboratory,
                Map.of("name", "Fridge 2-8 °C", "thresholds", FRIDGE), activeProfileId);
        expect(amended, 200);

        Answer activation = call(HttpMethod.POST, "/v1/storage-profiles/{profileId}/activation",
                laboratory, null, activeProfileId);
        expect(activation, 200);
        assertThat(data(activation).get("status").asString()).isEqualTo("ACTIVE");

        Answer superseded = call(HttpMethod.POST, "/v1/storage-profiles", laboratory, Map.of(
                "code", "FREEZER-" + suffix(), "name", "Freezer -20", "thresholds", FRIDGE));
        expect(superseded, 201);
        UUID supersededId = uuidAt(superseded, "id");
        expect(call(HttpMethod.POST, "/v1/storage-profiles/{profileId}/activation", laboratory, null,
                supersededId), 200);

        Answer next = call(HttpMethod.POST, "/v1/storage-profiles/{profileId}/versions", laboratory,
                null, supersededId);
        expect(next, 201);
        assertThat(data(next).get("version").asInt())
                .describedAs("a new version is the only way to change a threshold without changing "
                        + "what a shipment already in flight is judged against")
                .isEqualTo(2);
        assertThat(data(next).get("status").asString())
                .describedAs("the new version is born a draft: it does not take effect by appearing")
                .isEqualTo("DRAFT");

        Answer profiles = call(HttpMethod.GET, "/v1/storage-profiles", laboratory, null);
        expect(profiles, 200);

        Answer product = call(HttpMethod.POST, "/v1/products", laboratory, Map.of(
                "sku", "VAC-" + suffix(), "name", "Influenza vaccine",
                "storageProfileId", activeProfileId));
        expect(product, 201);
        productId = uuidAt(product, "id");

        Answer renamedProduct = call(HttpMethod.PUT, "/v1/products/{productId}", laboratory, Map.of(
                "name", "Influenza vaccine, quadrivalent",
                "storageProfileId", activeProfileId), productId);
        expect(renamedProduct, 200);

        Answer products = call(HttpMethod.GET, "/v1/products", laboratory, null);
        expect(products, 200);

        retiredProductId = uuidAt(call(HttpMethod.POST, "/v1/products", laboratory, Map.of(
                "sku", "OLD-" + suffix(), "name", "Discontinued vaccine",
                "storageProfileId", activeProfileId)), "id");
        Answer retirement = call(HttpMethod.DELETE, "/v1/products/{productId}", laboratory, null,
                retiredProductId);
        expect(retirement, 200);
    }

    @Test
    @Order(5)
    void theShipmentTravels_changesHandsAndIsMeasured() {
        Answer creation = call(HttpMethod.POST, "/v1/shipments", laboratory, shipment());
        expect(creation, 201);
        shipmentId = uuidAt(creation, "id");

        Answer device = call(HttpMethod.POST, "/v1/devices", laboratory, Map.of(
                "serialNumber", "SN-" + suffix(), "model", "Tag-1", "firmware", "1.4.2",
                "samplingIntervalSeconds", 300,
                "calibratedAt", Instant.now().minus(30, ChronoUnit.DAYS)));
        expect(device, 201);
        deviceId = uuidAt(device, "id");

        Answer devices = call(HttpMethod.GET, "/v1/devices", laboratory, null);
        expect(devices, 200);

        Answer dispatch = call(HttpMethod.POST, "/v1/shipments/{shipmentId}/dispatch", laboratory,
                Map.of("deviceId", deviceId), shipmentId);
        expect(dispatch, 200);
        assertThat(data(dispatch).get("status").asString()).isEqualTo("IN_TRANSIT");
        dispatchedAt = Instant.parse(data(dispatch).get("dispatchedAt").asString());

        Answer assignment = call(HttpMethod.POST, "/v1/devices/{deviceId}/assignment", laboratory,
                Map.of("shipmentId", shipmentId, "minCelsius", new BigDecimal("2.00"),
                        "maxCelsius", new BigDecimal("8.00"), "attachedAt", dispatchedAt), deviceId);
        expect(assignment, 201);

        Answer joined = call(HttpMethod.POST, "/v1/shipments/{shipmentId}/participants", laboratory,
                Map.of("participantOrganizationId", warehouseId, "participation", "CARRIER"),
                shipmentId);
        expect(joined, 201);

        Answer left = call(HttpMethod.DELETE,
                "/v1/shipments/{shipmentId}/participants/{organizationId}", laboratory, null,
                shipmentId, warehouseId);
        expect(left, 200);

        Answer withdrawn = call(HttpMethod.POST, "/v1/shipments/{shipmentId}/handoffs", laboratory,
                Map.of("toOrganizationId", carrierId), shipmentId);
        expect(withdrawn, 201);
        Answer rejection = call(HttpMethod.DELETE, "/v1/shipments/{shipmentId}/handoffs", laboratory,
                null, shipmentId);
        expect(rejection, 200);

        Answer handoff = call(HttpMethod.POST, "/v1/shipments/{shipmentId}/handoffs", laboratory,
                Map.of("toOrganizationId", carrierId), shipmentId);
        Answer acceptance = call(HttpMethod.POST, "/v1/shipments/{shipmentId}/handoffs/acceptance",
                carrier, Map.of("code", data(handoff).get("code").asString()), shipmentId);
        expect(acceptance, 200);
        assertThat(data(acceptance).get("currentCustodianOrganizationId").asString())
                .isEqualTo(carrierId.toString());

        Answer batch = call(HttpMethod.POST, "/v1/telemetry/batches", laboratory, Map.of(
                "deviceId", deviceId, "idempotencyKey", "contract-" + suffix(),
                "readings", readings()));
        expect(batch, 202);
        assertThat(data(batch).get("acceptedCount").asInt()).isEqualTo(12);

        Answer series = call(HttpMethod.GET, "/v1/shipments/{shipmentId}/series", laboratory, null,
                shipmentId);
        expect(series, 200);
        assertThat(data(series).get("points").size()).isEqualTo(12);

        Answer one = call(HttpMethod.GET, "/v1/shipments/{shipmentId}", carrier, null, shipmentId);
        expect(one, 200);

        Answer timeline = call(HttpMethod.GET, "/v1/shipments/{shipmentId}/timeline", laboratory,
                null, shipmentId);
        expect(timeline, 200);

        Answer listing = call(HttpMethod.GET, "/v1/shipments", hospital, null);
        expect(listing, 200);
        assertThat(data(listing).size())
                .describedAs("the consignee sees the shipment addressed to it and nothing else")
                .isEqualTo(1);
    }

    @Test
    @Order(6)
    void deliveryClosesTheJourneyAndTheCertificateIsAlreadyThere() {
        Answer arrival = call(HttpMethod.POST, "/v1/shipments/{shipmentId}/arrival", carrier, null,
                shipmentId);
        expect(arrival, 200);

        Answer delivery = call(HttpMethod.POST, "/v1/shipments/{shipmentId}/delivery", hospital,
                null, shipmentId);
        expect(delivery, 200);
        assertThat(data(delivery).get("status").asString()).isEqualTo("DELIVERED");

        Answer certificate = call(HttpMethod.GET, "/v1/shipments/{shipmentId}/certificate",
                laboratory, null, shipmentId);
        expect(certificate, 200);
        assertThat(data(certificate).get("version").asInt())
                .describedAs("closing the shipment issues the certificate: nobody has to remember to")
                .isEqualTo(1);
        assertThat(data(certificate).get("contentHash").asString()).hasSize(64);

        Answer reissued = call(HttpMethod.POST, "/v1/shipments/{shipmentId}/certificate", laboratory,
                null, shipmentId);
        expect(reissued, 201);
        assertThat(data(reissued).get("version").asInt()).isEqualTo(2);

        Answer detachment = call(HttpMethod.DELETE, "/v1/devices/{deviceId}/assignment", laboratory,
                null, deviceId);
        expect(detachment, 200);
    }

    @Test
    @Order(7)
    void theJourneysThatEndBadlyAreAlsoEndings() {
        UUID rejected = uuidAt(call(HttpMethod.POST, "/v1/shipments", laboratory, shipment()), "id");
        call(HttpMethod.POST, "/v1/shipments/{shipmentId}/dispatch", laboratory,
                Map.of("deviceId", deviceId), rejected);
        call(HttpMethod.POST, "/v1/shipments/{shipmentId}/arrival", laboratory, null, rejected);
        Answer refusal = call(HttpMethod.POST, "/v1/shipments/{shipmentId}/rejection", hospital,
                null, rejected);
        expect(refusal, 200);
        assertThat(data(refusal).get("status").asString()).isEqualTo("REJECTED");

        UUID abandoned = uuidAt(call(HttpMethod.POST, "/v1/shipments", laboratory, shipment()), "id");
        Answer cancellation = call(HttpMethod.POST, "/v1/shipments/{shipmentId}/cancellation",
                laboratory, null, abandoned);
        expect(cancellation, 200);
        assertThat(data(cancellation).get("status").asString()).isEqualTo("CANCELLED");
    }

    @Test
    @Order(8)
    void everyPublishedEndpointAnswersOverHttp() {
        Set<String> published = published();
        Set<String> unanswered = new LinkedHashSet<>(published);
        unanswered.removeAll(answered);

        assertThat(published).describedAs("there are endpoints to exercise").isNotEmpty();
        assertThat(unanswered)
                .describedAs("an endpoint nobody calls is one where a scope, a path or a field name "
                        + "can change and the build stays green")
                .isEmpty();
    }

    private Set<String> published() {
        Set<String> endpoints = new TreeSet<>();
        routes.getHandlerMethods().keySet().forEach(mapping -> {
            if (mapping.getPathPatternsCondition() == null) {
                return;
            }
            mapping.getPathPatternsCondition().getPatternValues().stream()
                    .filter(pattern -> pattern.startsWith("/v1/"))
                    .forEach(pattern -> mapping.getMethodsCondition().getMethods()
                            .forEach(method -> endpoints.add(method.name() + " " + pattern)));
        });
        return endpoints;
    }

    private Answer register(String slug, String legalName, String kind) {
        return call(HttpMethod.POST, "/v1/organizations", null, Map.of(
                "taxId", slug + "-" + suffix(),
                "legalName", legalName,
                "tradeName", legalName,
                "kind", kind,
                "country", "UY",
                "administratorEmail", slug + "-" + suffix() + "@coldchain.dev",
                "administratorFullName", legalName + " administrator",
                "administratorPassword", PASSWORD));
    }

    private String accessTokenOf(Answer registration) {
        Answer login = call(HttpMethod.POST, "/v1/auth/login", null, Map.of(
                "email", data(registration).get("email").asString(),
                "password", PASSWORD));
        expect(login, 201);

        Answer refreshed = call(HttpMethod.POST, "/v1/auth/refresh", null,
                Map.of("refreshToken", data(login).get("refreshToken").asString()));
        expect(refreshed, 201);
        return data(refreshed).get("accessToken").asString();
    }

    private Map<String, Object> site(String prefix, String kind) {
        return Map.of("code", prefix + "-" + suffix(), "name", prefix + " site", "kind", kind,
                "latitude", new BigDecimal("-34.90"), "longitude", new BigDecimal("-56.16"),
                "timeZone", "America/Montevideo");
    }

    private Map<String, Object> shipment() {
        return Map.of("reference", "SHP-" + suffix(),
                "originSiteId", originSiteId,
                "destinationSiteId", destinationSiteId,
                "consigneeOrganizationId", hospitalId,
                "lines", List.of(Map.of("productId", productId,
                        "quantity", new BigDecimal("400.000"), "unit", "VIAL")));
    }

    private List<Map<String, Object>> readings() {
        List<Map<String, Object>> readings = new ArrayList<>();
        for (int minute = 0; minute < 60; minute += 5) {
            readings.add(Map.of("measuredAt", dispatchedAt.plus(minute, ChronoUnit.MINUTES),
                    "celsius", new BigDecimal("4.50")));
        }
        return readings;
    }

    private Answer call(HttpMethod method, String template, String token, Object body,
            Object... variables) {
        answered.add(method.name() + " " + template);
        RestClient.RequestBodySpec request = client()
                .method(method)
                .uri(CONTEXT_PATH + template, variables)
                .accept(MediaType.APPLICATION_JSON);
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON);
            request = request.body(json.writeValueAsString(body));
        }
        ResponseEntity<String> response = request.retrieve().toEntity(String.class);
        return new Answer(response.getStatusCode().value(),
                response.getBody() == null ? json.createObjectNode() : json.readTree(response.getBody()));
    }

    private RestClient client() {
        if (http == null) {
            http = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .defaultStatusHandler(status -> true, (request, response) -> {
                    })
                    .build();
        }
        return http;
    }

    private void expect(Answer answer, int status) {
        assertThat(answer.status())
                .describedAs("the API answered %s", answer.body())
                .isEqualTo(status);
    }

    private JsonNode data(Answer answer) {
        assertThat(answer.body().has("data"))
                .describedAs("every answer travels in the same envelope: %s", answer.body())
                .isTrue();
        return answer.body().get("data");
    }

    private UUID uuidAt(Answer answer, String field) {
        return UUID.fromString(data(answer).get(field).asString());
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record Answer(int status, JsonNode body) {
    }
}
