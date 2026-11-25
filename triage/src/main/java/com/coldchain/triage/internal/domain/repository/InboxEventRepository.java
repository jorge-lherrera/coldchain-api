package com.coldchain.triage.internal.domain.repository;

import com.coldchain.triage.internal.domain.model.InboxEvent;
import java.util.Optional;
import java.util.UUID;

public interface InboxEventRepository {

    Optional<InboxEvent> findById(UUID eventId);

    InboxEvent save(InboxEvent event);
}
