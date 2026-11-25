package com.coldchain.shared.infrastructure.events;

import java.time.Duration;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventPublicationResubmitter {

    private static final Duration GRACE = Duration.ofMinutes(1);

    private final IncompleteEventPublications publications;

    public EventPublicationResubmitter(IncompleteEventPublications publications) {
        this.publications = publications;
    }

    @Scheduled(fixedDelayString = "${coldchain.events.resubmit-every:1m}")
    void resubmitStalled() {
        publications.resubmitIncompletePublicationsOlderThan(GRACE);
    }
}
