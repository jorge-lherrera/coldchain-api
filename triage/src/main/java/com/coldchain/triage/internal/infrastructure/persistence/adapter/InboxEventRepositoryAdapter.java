package com.coldchain.triage.internal.infrastructure.persistence.adapter;

import com.coldchain.triage.internal.domain.model.InboxEvent;
import com.coldchain.triage.internal.domain.repository.InboxEventRepository;
import com.coldchain.triage.internal.infrastructure.persistence.jpa.InboxEventJpaRepository;
import com.coldchain.triage.internal.infrastructure.persistence.mapper.TriagePersistenceMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class InboxEventRepositoryAdapter implements InboxEventRepository {

    private final InboxEventJpaRepository events;

    private final TriagePersistenceMapper mapper;

    public InboxEventRepositoryAdapter(InboxEventJpaRepository events, TriagePersistenceMapper mapper) {
        this.events = events;
        this.mapper = mapper;
    }

    @Override
    public Optional<InboxEvent> findById(UUID eventId) {
        return events.findById(eventId).map(mapper::toDomain);
    }

    @Override
    public InboxEvent save(InboxEvent event) {
        events.saveAndFlush(mapper.toEntity(event));
        return event;
    }
}
