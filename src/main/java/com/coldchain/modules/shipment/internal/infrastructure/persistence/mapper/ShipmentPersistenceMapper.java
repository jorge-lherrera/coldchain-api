package com.coldchain.modules.shipment.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.shipment.api.ShipmentStatus;
import com.coldchain.modules.shipment.internal.domain.model.FrozenThresholds;
import com.coldchain.modules.shipment.internal.domain.model.Shipment;
import com.coldchain.modules.shipment.internal.domain.model.ShipmentLine;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.entity.ShipmentJpaEntity;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.entity.ShipmentLineJpaEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShipmentPersistenceMapper {

    public ShipmentJpaEntity toEntity(Shipment shipment) {
        FrozenThresholds thresholds = shipment.thresholds();
        return new ShipmentJpaEntity(shipment.id(), shipment.organizationId(), shipment.reference(),
                shipment.status().name(), shipment.originSiteId(), shipment.destinationSiteId(),
                shipment.consigneeOrganizationId(), shipment.currentCustodianOrganizationId(),
                shipment.deviceId(),
                thresholds == null ? null : thresholds.minCelsius(),
                thresholds == null ? null : thresholds.maxCelsius(),
                thresholds == null ? null : thresholds.maxSingleExcursionMinutes(),
                thresholds == null ? null : thresholds.maxCumulativeExcursionMinutes(),
                thresholds == null ? null : thresholds.minCoveragePercent(),
                shipment.openExcursion() ? 1 : 0, shipment.dispatchedAt(), shipment.closedAt(), shipment.lockVersion());
    }

    public ShipmentLineJpaEntity toEntity(ShipmentLine line) {
        return new ShipmentLineJpaEntity(line.id(), line.shipmentId(), line.productId(),
                line.productName(), line.profileVersion(), line.quantity(), line.unit());
    }

    public Shipment toDomain(ShipmentJpaEntity entity, List<ShipmentLineJpaEntity> lines) {
        return Shipment.restore(entity.getId(), entity.getOrganizationId(), entity.getReference(),
                ShipmentStatus.valueOf(entity.getStatus()), entity.getOriginSiteId(),
                entity.getDestinationSiteId(), entity.getConsigneeOrganizationId(),
                entity.getCurrentCustodianOrganizationId(), entity.getDeviceId(),
                toThresholds(entity), entity.getOpenExcursion() == 1, entity.getDispatchedAt(),
                entity.getClosedAt(), lines.stream().map(this::toDomain).toList(), entity.getLockVersion());
    }

    public ShipmentLine toDomain(ShipmentLineJpaEntity entity) {
        return ShipmentLine.restore(entity.getId(), entity.getShipmentId(), entity.getProductId(),
                entity.getProductName(), entity.getProfileVersion(), entity.getQuantity(),
                entity.getUnit());
    }

    private FrozenThresholds toThresholds(ShipmentJpaEntity entity) {
        if (entity.getMinCelsius() == null) {
            return null;
        }
        return new FrozenThresholds(entity.getMinCelsius(), entity.getMaxCelsius(),
                entity.getMaxSingleExcursionMinutes(), entity.getMaxCumulativeExcursionMinutes(),
                entity.getMinCoveragePercent());
    }
}
