package com.coldchain.modules.shipment.internal.application.usecase.query;

import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.internal.application.mapper.ShipmentApiMapper;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.UUID;

@UseCase
public class ListShipmentsUseCase {

    private final ShipmentRepository shipments;

    private final ShipmentApiMapper mapper;

    public ListShipmentsUseCase(ShipmentRepository shipments, ShipmentApiMapper mapper) {
        this.shipments = shipments;
        this.mapper = mapper;
    }

    public PagedResult<ShipmentResult> execute(UUID viewerOrganizationId, PageCriteria criteria) {
        return shipments.findVisibleTo(viewerOrganizationId, criteria).map(mapper::toResult);
    }
}
