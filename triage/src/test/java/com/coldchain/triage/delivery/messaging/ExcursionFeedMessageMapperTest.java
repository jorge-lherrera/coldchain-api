package com.coldchain.triage.delivery.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.triage.api.ExcursionEventType;
import com.coldchain.triage.api.ExcursionKind;
import com.coldchain.triage.api.dto.RecordExcursionEventCommand;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class ExcursionFeedMessageMapperTest {

    private static final String OPENED_AS_THE_API_PUBLISHES_IT = """
            {"eventId":"1cde30db-ae46-36e4-8c4d-f786ef34d362","type":"ExcursionOpened",\
            "occurredAt":"2026-11-25T12:49:15Z","shipmentId":"01a21289-dd9b-7000-bcf9-3a39ebebc64a",\
            "organizationId":"01a21289-d0c9-7000-b517-bcec4a296dc9",\
            "excursionId":"01a21289-e53a-7000-889f-c680e81a1f86","kind":"ABOVE_MAX","peakCelsius":11.5,\
            "durationMinutes":null}""";

    private final ExcursionFeedMessageMapper mapper = new ExcursionFeedMessageMapper(JsonMapper.builder().build());

    @Test
    void readsTheMessageTheApiPublishes() {
        RecordExcursionEventCommand command = mapper.toCommand(OPENED_AS_THE_API_PUBLISHES_IT);

        assertThat(command.eventId()).isEqualTo(UUID.fromString("1cde30db-ae46-36e4-8c4d-f786ef34d362"));
        assertThat(command.type()).isEqualTo(ExcursionEventType.OPENED);
        assertThat(command.occurredAt()).isEqualTo(Instant.parse("2026-11-25T12:49:15Z"));
        assertThat(command.kind()).isEqualTo(ExcursionKind.ABOVE_MAX);
        assertThat(command.peakCelsius()).isEqualByComparingTo("11.5");
        assertThat(command.durationMinutes()).isNull();
    }

    @Test
    void aFieldItDoesNotKnowIsIgnored() {
        String withANewField = OPENED_AS_THE_API_PUBLISHES_IT.replace("\"kind\"", "\"carrierHint\":\"x\",\"kind\"");

        assertThat(mapper.toCommand(withANewField).type())
                .describedAs("a backward-compatible field on v1 must not break a consumer that predates it")
                .isEqualTo(ExcursionEventType.OPENED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "{not json",
        "[]",
        "{\"eventId\":\"1cde30db-ae46-36e4-8c4d-f786ef34d362\"}",
        "{\"eventId\":\"1cde30db-ae46-36e4-8c4d-f786ef34d362\",\"type\":\"ExcursionMoved\","
                + "\"occurredAt\":\"2026-11-25T12:49:15Z\",\"shipmentId\":\"01a21289-dd9b-7000-bcf9-3a39ebebc64a\","
                + "\"organizationId\":\"01a21289-d0c9-7000-b517-bcec4a296dc9\","
                + "\"excursionId\":\"01a21289-e53a-7000-889f-c680e81a1f86\",\"kind\":\"ABOVE_MAX\",\"peakCelsius\":11.5}",
        "{\"eventId\":\"1cde30db-ae46-36e4-8c4d-f786ef34d362\",\"type\":\"ExcursionOpened\","
                + "\"occurredAt\":\"2026-11-25T12:49:15Z\",\"shipmentId\":\"01a21289-dd9b-7000-bcf9-3a39ebebc64a\","
                + "\"organizationId\":\"01a21289-d0c9-7000-b517-bcec4a296dc9\","
                + "\"excursionId\":\"01a21289-e53a-7000-889f-c680e81a1f86\",\"kind\":\"TOO_WARM\",\"peakCelsius\":11.5}",
        "{\"eventId\":\"1cde30db-ae46-36e4-8c4d-f786ef34d362\",\"type\":\"ExcursionOpened\","
                + "\"occurredAt\":\"2026-11-25T12:49:15Z\",\"shipmentId\":\"01a21289-dd9b-7000-bcf9-3a39ebebc64a\","
                + "\"organizationId\":\"01a21289-d0c9-7000-b517-bcec4a296dc9\","
                + "\"excursionId\":\"01a21289-e53a-7000-889f-c680e81a1f86\",\"kind\":\"ABOVE_MAX\"}"
    })
    void aMessageThatCanNeverBeProcessedIsUnreadable(String payload) {
        assertThatThrownBy(() -> mapper.toCommand(payload))
                .describedAs("the error handler dead-letters this exception without retrying it")
                .isInstanceOf(UnreadableFeedMessageException.class);
    }
}
