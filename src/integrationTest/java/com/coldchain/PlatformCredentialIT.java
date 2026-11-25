package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("integration")
class PlatformCredentialIT {

    private static final String CONTEXT_PATH = "/api";

    private static final String PLATFORM_CLIENT_ID = "coldchain-triage";

    private static final String PLATFORM_SECRET = "integration-platform-" + UUID.randomUUID();

    private static final String PASSWORD = "integration-password";

    private static final String INTERNAL_PROBE = "/internal/v1/probe";

    private static final Set<String> OPEN_TO_EVERYONE = Set.of(
            "POST /v1/organizations", "POST /v1/auth/login", "POST /v1/auth/refresh",
            "POST /v1/auth/client-token", "POST /v1/auth/activation");

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JwtDecoder tokens;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping routes;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void platformClient(DynamicPropertyRegistry registry) {
        registry.add("coldchain.platform.client-id", () -> PLATFORM_CLIENT_ID);
        registry.add("coldchain.platform.client-secret-hash",
                () -> Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(PLATFORM_SECRET));
    }

    @Test
    void thePlatformClientGetsATokenThatBelongsToNoOrganization() {
        Answer issued = call(HttpMethod.POST, "/v1/auth/client-token", null,
                Map.of("clientId", PLATFORM_CLIENT_ID, "clientSecret", PLATFORM_SECRET));

        assertThat(issued.status()).describedAs("the API answered %s", issued.body()).isEqualTo(201);
        Jwt token = tokens.decode(issued.body().get("data").get("accessToken").asString());
        assertThat(token.getClaims())
                .describedAs("the platform takes part in no shipment, so its token names no tenant")
                .doesNotContainKey("org");
        assertThat(token.getClaimAsString("typ")).isEqualTo("PLATFORM");
        assertThat(token.getClaimAsString("scope")).isEqualTo("PLATFORM_EXCURSION_READ");
    }

    @Test
    void aWrongPlatformSecretGetsNoToken() {
        Answer refused = call(HttpMethod.POST, "/v1/auth/client-token", null,
                Map.of("clientId", PLATFORM_CLIENT_ID, "clientSecret", "not-the-secret"));

        assertThat(refused.status()).isEqualTo(401);
    }

    @Test
    void anOrganizationTokenIsRefusedOnTheInternalSurface() {
        Answer denied = call(HttpMethod.GET, INTERNAL_PROBE, organizationToken(), null);

        assertThat(denied.status())
                .describedAs("a tenant's token reads what its organization takes part in, never every excursion")
                .isEqualTo(403);
    }

    @Test
    void aCallWithNoTokenIsRefusedOnTheInternalSurface() {
        assertThat(call(HttpMethod.GET, INTERNAL_PROBE, null, null).status()).isEqualTo(401);
    }

    @Test
    void thePlatformTokenGetsThroughTheInternalChain() {
        Answer probed = call(HttpMethod.GET, INTERNAL_PROBE, platformToken(), null);

        assertThat(probed.status())
                .describedAs("past the filter chain there is no handler yet; whatever answers, it is not a denial")
                .isNotIn(401, 403);
    }

    @Test
    void thePlatformTokenIsRefusedOnEveryPublicRoute() {
        String platform = platformToken();
        List<String> published = publishedRoutes();

        assertThat(published).isNotEmpty();
        List<String> letThrough = new ArrayList<>();
        for (String route : published) {
            int space = route.indexOf(' ');
            HttpMethod method = HttpMethod.valueOf(route.substring(0, space));
            String path = route.substring(space + 1).replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
            int status = call(method, path, platform, null).status();
            if (status != 403) {
                letThrough.add(route + " -> " + status);
            }
        }
        assertThat(letThrough)
                .describedAs("the platform token enters /internal/** and nothing else (ADR-011)")
                .isEmpty();
    }

    private List<String> publishedRoutes() {
        List<String> published = new ArrayList<>();
        for (RequestMappingInfo info : routes.getHandlerMethods().keySet()) {
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (String pattern : info.getPatternValues()) {
                if (pattern.startsWith("/error") || pattern.startsWith("/v3/api-docs")
                        || pattern.startsWith("/swagger-ui")) {
                    continue;
                }
                for (RequestMethod method : methods.isEmpty() ? Set.of(RequestMethod.GET) : methods) {
                    String route = method.name() + " " + pattern;
                    if (!OPEN_TO_EVERYONE.contains(route)) {
                        published.add(route);
                    }
                }
            }
        }
        return published;
    }

    private String platformToken() {
        Answer issued = call(HttpMethod.POST, "/v1/auth/client-token", null,
                Map.of("clientId", PLATFORM_CLIENT_ID, "clientSecret", PLATFORM_SECRET));
        assertThat(issued.status()).isEqualTo(201);
        return issued.body().get("data").get("accessToken").asString();
    }

    private String organizationToken() {
        String slug = "lab-" + UUID.randomUUID().toString().substring(0, 8);
        Answer registration = call(HttpMethod.POST, "/v1/organizations", null, Map.of(
                "taxId", slug,
                "legalName", "Platform Probe Laboratory",
                "tradeName", "Platform Probe Laboratory",
                "kind", "LAB",
                "country", "UY",
                "administratorEmail", slug + "@coldchain.dev",
                "administratorFullName", "Platform Probe administrator",
                "administratorPassword", PASSWORD));
        assertThat(registration.status()).describedAs("the API answered %s", registration.body()).isEqualTo(201);
        Answer login = call(HttpMethod.POST, "/v1/auth/login", null, Map.of(
                "email", registration.body().get("data").get("email").asString(),
                "password", PASSWORD));
        assertThat(login.status()).isEqualTo(201);
        return login.body().get("data").get("accessToken").asString();
    }

    private Answer call(HttpMethod method, String path, String token, Object body) {
        RestClient.RequestBodySpec request = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (sent, response) -> {
                })
                .build()
                .method(method)
                .uri(CONTEXT_PATH + path)
                .accept(MediaType.APPLICATION_JSON);
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).body(json.writeValueAsString(body));
        }
        ResponseEntity<String> response = request.retrieve().toEntity(String.class);
        String text = response.getBody();
        return new Answer(response.getStatusCode().value(),
                text == null || text.isBlank() ? json.createObjectNode() : json.readTree(text));
    }

    private record Answer(int status, JsonNode body) {
    }
}
