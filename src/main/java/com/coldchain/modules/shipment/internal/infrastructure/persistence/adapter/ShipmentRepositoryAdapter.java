package com.coldchain.modules.shipment.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.shipment.internal.domain.model.Shipment;
import com.coldchain.modules.shipment.internal.domain.model.ShipmentLine;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.entity.ShipmentLineJpaEntity;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.jpa.ShipmentJpaRepository;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.jpa.ShipmentLineJpaRepository;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.mapper.ShipmentPersistenceMapper;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import com.coldchain.shared.paging.SpringDataPaging;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class ShipmentRepositoryAdapter implements ShipmentRepository {

    private final ShipmentJpaRepository shipments;

    private final ShipmentLineJpaRepository lines;

    private final ShipmentPersistenceMapper mapper;

    public ShipmentRepositoryAdapter(ShipmentJpaRepository shipments, ShipmentLineJpaRepository lines,
            ShipmentPersistenceMapper mapper) {
        this.shipments = shipments;
        this.lines = lines;
        this.mapper = mapper;
    }

    @Override
    public Shipment save(Shipment shipment) {
        try {
            shipments.saveAndFlush(mapper.toEntity(shipment));
            List<ShipmentLineJpaEntity> stored = lines.findByShipmentId(shipment.id());
            List<UUID> keep = shipment.lines().stream().map(ShipmentLine::id).toList();
            stored.stream().filter(line -> !keep.contains(line.getId())).forEach(lines::delete);
            shipment.lines().stream().map(mapper::toEntity).forEach(lines::save);
            lines.flush();
        } catch (DataIntegrityViolationException cause) {
            throw ShipmentConstraintTranslation.translate(cause);
        }
        return shipment;
    }

    @Override
    public Optional<Shipment> findVisible(UUID shipmentId, UUID viewerOrganizationId) {
        return shipments.findVisible(shipmentId, viewerOrganizationId)
                .map(entity -> mapper.toDomain(entity, lines.findByShipmentId(entity.getId())));
    }

    @Override
    public PagedResult<Shipment> findVisibleTo(UUID viewerOrganizationId, PageCriteria criteria) {
        return SpringDataPaging.toPagedResult(
                shipments.findVisibleTo(viewerOrganizationId, SpringDataPaging.toPageable(criteria))
                        .map(entity -> mapper.toDomain(entity, lines.findByShipmentId(entity.getId()))),
                criteria);
    }
}
