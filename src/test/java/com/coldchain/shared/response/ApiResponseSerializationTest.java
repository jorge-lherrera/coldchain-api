package com.coldchain.shared.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@JsonTest
class ApiResponseSerializationTest {

    private static final String[] ENVELOPE_FIELDS = {
        "status", "code", "messageKey", "message", "data", "traceId", "timestamp", "meta"
    };

    private final ObjectMapper objectMapper;

    ApiResponseSerializationTest(@Autowired ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Test
    void theEnvelopeCarriesItsEightFieldsAndNothingElse() {
        Map<String, Object> json = serialise(new ApiResponse<>(200, "SHIPMENT_RETRIEVED",
                "success.shipment.retrieved", "Shipment retrieved", "payload", "trace-1",
                Instant.parse("2026-09-17T10:15:30Z"), ResponseMeta.empty()));

        assertThat(json.keySet()).containsExactlyInAnyOrder(ENVELOPE_FIELDS);
    }

    @Test
    void thereIsNoSuccessFlagBecauseTheStatusAlreadySaysIt() {
        String json = objectMapper.writeValueAsString(new ApiResponse<>(200, "X", "success.core.x", "X", null,
                "trace-1", Instant.EPOCH, ResponseMeta.empty()));

        assertThat(json).doesNotContain("\"success\"");
    }

    @Test
    void aNullFieldIsStillSerialisedSoATypedClientNeverBranchesOnPresence() {
        Map<String, Object> json = serialise(new ApiResponse<>(204, "X", "success.core.x", "X", null, "trace-1",
                Instant.EPOCH, ResponseMeta.empty()));

        assertThat(json.keySet()).containsExactlyInAnyOrder(ENVELOPE_FIELDS);
        assertThat(json).containsEntry("data", null);
        assertThat(json.get("meta")).isEqualTo(Collections.singletonMap("pagination", null));
    }

    @Test
    void theTimestampGoesOutAsAnIsoInstantAndNotAsANumber() {
        String json = objectMapper.writeValueAsString(new ApiResponse<>(200, "X", "success.core.x", "X", null,
                "trace-1", Instant.parse("2026-09-17T10:15:30Z"), ResponseMeta.empty()));

        assertThat(json).contains("\"2026-09-17T10:15:30Z\"");
    }

    private Map<String, Object> serialise(ApiResponse<?> response) {
        return objectMapper.readValue(objectMapper.writeValueAsString(response),
                new TypeReference<Map<String, Object>>() {});
    }
}
