package com.coldchain.triage.delivery.messaging;

import com.coldchain.triage.api.TriageApi;
import com.coldchain.triage.api.dto.RecordExcursionEventResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
    public void onExcursionEvent(String payload) {
        RecordExcursionEventResult result = triage.recordExcursionEvent(mapper.toCommand(payload));
        LOG.info("Excursion event {} of excursion {}: {}", result.eventId(), result.excursionId(),
                result.outcome());
    }
}
