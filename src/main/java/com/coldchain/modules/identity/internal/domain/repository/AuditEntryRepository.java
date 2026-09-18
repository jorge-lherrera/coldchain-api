package com.coldchain.modules.identity.internal.domain.repository;

import com.coldchain.modules.identity.internal.domain.model.AuditEntry;

public interface AuditEntryRepository {

    AuditEntry save(AuditEntry entry);
}
