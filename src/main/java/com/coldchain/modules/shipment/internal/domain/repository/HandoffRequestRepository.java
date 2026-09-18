package com.coldchain.modules.shipment.internal.domain.repository;

import com.coldchain.modules.shipment.internal.domain.model.HandoffRequest;
import java.util.Optional;
import java.util.UUID;

public interface HandoffRequestRepository {

    HandoffRequest save(HandoffRequest request);

    Optional<HandoffRequest> findPending(UUID shipmentId);
}
