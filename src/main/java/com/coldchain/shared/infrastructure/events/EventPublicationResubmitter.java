package com.coldchain.shared.infrastructure.events;

import java.time.Duration;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Resubmits the publications that failed, once they are a minute old. A publication that never fails
 * but never completes either is marked failed by the registry's staleness check, configured with the
 * same minute, so it is resubmitted here too.
 */
@Component
public class EventPublicationResubmitter {

    private static final ResubmissionOptions A_MINUTE_OLD =
            ResubmissionOptions.defaults().withMinAge(Duration.ofMinutes(1));

    private final FailedEventPublications publications;

    public EventPublicationResubmitter(FailedEventPublications publications) {
        this.publications = publications;
    }

    @Scheduled(fixedDelayString = "${coldchain.events.resubmit-every:1m}")
    void resubmitStalled() {
        publications.resubmit(A_MINUTE_OLD);
    }
}
