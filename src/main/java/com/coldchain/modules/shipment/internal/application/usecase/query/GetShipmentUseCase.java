package com.coldchain.modules.shipment.internal.application.usecase.query;

import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.internal.application.mapper.ShipmentApiMapper;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.modules.shipment.internal.exception.ShipmentErrorCode;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import java.util.UUID;

@UseCase
public class GetShipmentUseCase {

    private final ShipmentRepository shipments;

    private final ShipmentApiMapper mapper;

    public GetShipmentUseCase(ShipmentRepository shipments, ShipmentApiMapper mapper) {
        this.shipments = shipments;
        this.mapper = mapper;
    }

    public ShipmentResult execute(UUID shipmentId, UUID viewerOrganizationId) {
        return shipments.findVisible(shipmentId, viewerOrganizationId).map(mapper::toResult)
                .orElseThrow(() -> DomainException.of(ShipmentErrorCode.SHIPMENT_NOT_FOUND));
    }
}
