package com.coldchain.modules.shipment.internal.domain.repository;

import com.coldchain.modules.shipment.internal.domain.model.CustodyEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustodyEventRepository {

    CustodyEvent append(CustodyEvent event);

    List<CustodyEvent> findOrderedBySequence(UUID shipmentId);

    Optional<CustodyEvent> findLast(UUID shipmentId);
}
