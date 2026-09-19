package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("integration")
class MessageLocaleIT {

    private static final String PASSWORD = "integration-password";

    @Autowired
    private ObjectMapper json;

    @LocalServerPort
    private int port;

    @Test
    void theAnswerComesBackInTheLanguageTheCallerAsksFor() {
        String taxId = "locale-" + UUID.randomUUID().toString().substring(0, 8);

        JsonNode inEnglish = register(taxId, null);
        JsonNode inSpanish = register(taxId, "es");
        JsonNode inPortuguese = register(taxId, "pt");

        assertThat(inEnglish.get("message").asString()).isEqualTo("Organization registered");
        assertThat(inSpanish.get("title").asString())
                .describedAs("the second registration repeats the tax identifier, and the refusal "
                        + "has to arrive in the language the caller asked for")
                .isEqualTo("Ya existe una organización con ese identificador fiscal");
        assertThat(inPortuguese.get("title").asString())
                .isEqualTo("Já existe uma organização com esse identificador fiscal");
        assertThat(inSpanish.get("messageKey").asString())
                .describedAs("the key is the contract and does not move with the language")
                .isEqualTo(inPortuguese.get("messageKey").asString());
    }

    @Test
    void aLanguageTheApiDoesNotSpeakIsAnsweredInEnglish() {
        JsonNode answer = register("locale-" + UUID.randomUUID().toString().substring(0, 8), "de");

        assertThat(answer.get("message").asString())
                .describedAs("an unsupported language falls back to English instead of answering "
                        + "with the raw key")
                .isEqualTo("Organization registered");
    }

    private JsonNode register(String taxId, String language) {
        RestClient.RequestBodySpec request = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (ignored, alsoIgnored) -> {
                })
                .build()
                .method(HttpMethod.POST)
                .uri("/api/v1/organizations")
                .contentType(MediaType.APPLICATION_JSON);
        if (language != null) {
            request = request.header("Accept-Language", language);
        }
        ResponseEntity<String> response = request
                .body(json.writeValueAsString(Map.of(
                        "taxId", taxId,
                        "legalName", "Locale Test",
                        "tradeName", "Locale Test",
                        "kind", "SHIPPER",
                        "country", "UY",
                        "administratorEmail", taxId + "@coldchain.dev",
                        "administratorFullName", "Locale administrator",
                        "administratorPassword", PASSWORD)))
                .retrieve()
                .toEntity(String.class);
        return json.readTree(response.getBody());
    }
}
