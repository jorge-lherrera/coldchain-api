package com.coldchain.delivery.messaging.advisory;

import com.coldchain.modules.advisory.api.AdvisoryApi;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reads the record itself, not a converted payload: the bytes are parsed by this channel, so a message
 * that can never be read is named here and dead-lettered without a retry.
 */
@Component
public class AlertFeedListener {

    private final AdvisoryApi advisory;

    private final AlertFeedMessageMapper mapper;

    public AlertFeedListener(AdvisoryApi advisory, AlertFeedMessageMapper mapper) {
        this.advisory = advisory;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${coldchain.advisory.alert-topic}", groupId = "${coldchain.advisory.consumer-group}")
    public void onAlert(ConsumerRecord<String, byte[]> record) {
        String payload = record.value() == null ? "" : new String(record.value(), StandardCharsets.UTF_8);
        advisory.recordAlert(mapper.toCommand(payload));
    }
}
