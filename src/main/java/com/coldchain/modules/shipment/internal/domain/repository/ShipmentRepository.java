package com.coldchain.modules.shipment.internal.domain.repository;

import com.coldchain.modules.shipment.internal.domain.model.Shipment;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository {

    Shipment save(Shipment shipment);

    Optional<Shipment> findVisible(UUID shipmentId, UUID viewerOrganizationId);

    PagedResult<Shipment> findVisibleTo(UUID viewerOrganizationId, PageCriteria criteria);

    int markOpenExcursion(UUID shipmentId, boolean open);
}
