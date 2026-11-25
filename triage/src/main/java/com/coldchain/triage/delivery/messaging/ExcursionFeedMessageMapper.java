package com.coldchain.triage.delivery.messaging;

import com.coldchain.triage.api.ExcursionEventType;
import com.coldchain.triage.api.ExcursionKind;
import com.coldchain.triage.api.dto.RecordExcursionEventCommand;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ExcursionFeedMessageMapper {

    private final JsonMapper json;

    public ExcursionFeedMessageMapper(JsonMapper json) {
        this.json = json;
    }

    public RecordExcursionEventCommand toCommand(String payload) {
        ExcursionFeedMessage message = read(payload);
        requirePresent(message);
        ExcursionEventType type = ExcursionEventType.ofWireName(message.type()).orElseThrow(
                () -> new UnreadableFeedMessageException("Unknown event type " + message.type()));
        ExcursionKind kind = Arrays.stream(ExcursionKind.values())
                .filter(candidate -> candidate.name().equals(message.kind()))
                .findFirst()
                .orElseThrow(() -> new UnreadableFeedMessageException("Unknown excursion kind " + message.kind()));
        if (type == ExcursionEventType.OPENED && message.peakCelsius() == null) {
            throw new UnreadableFeedMessageException("An opened excursion without its peak: " + message.eventId());
        }
        if (type == ExcursionEventType.CLOSED && message.durationMinutes() == null) {
            throw new UnreadableFeedMessageException("A closed excursion without its duration: " + message.eventId());
        }
        return new RecordExcursionEventCommand(message.eventId(), type, message.occurredAt(), message.shipmentId(),
                message.organizationId(), message.excursionId(), kind, message.peakCelsius(),
                message.durationMinutes());
    }

    private ExcursionFeedMessage read(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new UnreadableFeedMessageException("An empty message");
        }
        try {
            return json.readValue(payload, ExcursionFeedMessage.class);
        } catch (JacksonException cause) {
            throw new UnreadableFeedMessageException("Not a feed message: " + cause.getOriginalMessage(), cause);
        }
    }

    private static void requirePresent(ExcursionFeedMessage message) {
        if (message.eventId() == null || message.type() == null || message.occurredAt() == null
                || message.shipmentId() == null || message.organizationId() == null
                || message.excursionId() == null || message.kind() == null) {
            throw new UnreadableFeedMessageException("A feed message without one of its required fields");
        }
    }
}
