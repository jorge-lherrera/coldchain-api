package com.coldchain.triage.delivery.messaging;

import com.coldchain.triage.api.TriageApi;
import com.coldchain.triage.api.dto.RecordExcursionEventResult;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reads the record itself, not a converted payload: the bytes are parsed here, so a message that can
 * never be read is named by this channel and dead-lettered without a retry, whatever the converter the
 * outbox configures for the producer side.
 */
@Component
public class ExcursionFeedListener {

    private static final Logger LOG = LoggerFactory.getLogger(ExcursionFeedListener.class);

    private final TriageApi triage;

    private final ExcursionFeedMessageMapper mapper;

    public ExcursionFeedListener(TriageApi triage, ExcursionFeedMessageMapper mapper) {
        this.triage = triage;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${coldchain.triage.feed-topic}")
    public void onExcursionEvent(ConsumerRecord<String, byte[]> record) {
        String payload = record.value() == null ? "" : new String(record.value(), StandardCharsets.UTF_8);
        RecordExcursionEventResult result = triage.recordExcursionEvent(mapper.toCommand(payload));
        LOG.info("Excursion event {} of excursion {}: {}", result.eventId(), result.excursionId(),
                result.outcome());
    }
}
