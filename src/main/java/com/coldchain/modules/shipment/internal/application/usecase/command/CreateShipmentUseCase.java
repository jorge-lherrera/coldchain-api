package com.coldchain.modules.shipment.internal.application.usecase.command;

import com.coldchain.modules.catalog.api.CatalogApi;
import com.coldchain.modules.catalog.api.dto.ProductResult;
import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.shipment.api.CustodyEventKind;
import com.coldchain.modules.shipment.api.Participation;
import com.coldchain.modules.shipment.api.dto.CreateShipmentCommand;
import com.coldchain.modules.shipment.api.dto.ShipmentLineCommand;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.internal.application.CustodyLog;
import com.coldchain.modules.shipment.internal.application.mapper.ShipmentApiMapper;
import com.coldchain.modules.shipment.internal.domain.model.Shipment;
import com.coldchain.modules.shipment.internal.domain.model.ShipmentLine;
import com.coldchain.modules.shipment.internal.domain.model.ShipmentParticipant;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentParticipantRepository;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.security.CurrentActor;
import java.time.Clock;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class CreateShipmentUseCase {

    private final ShipmentRepository shipments;

    private final ShipmentParticipantRepository participants;

    private final CatalogApi catalog;

    private final CustodyLog custodyLog;

    private final ShipmentApiMapper mapper;

    private final CurrentActor currentActor;

    private final Clock clock;

    public CreateShipmentUseCase(ShipmentRepository shipments,
            ShipmentParticipantRepository participants, CatalogApi catalog, CustodyLog custodyLog,
            ShipmentApiMapper mapper, CurrentActor currentActor, Clock clock) {
        this.shipments = shipments;
        this.participants = participants;
        this.catalog = catalog;
        this.custodyLog = custodyLog;
        this.mapper = mapper;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @Transactional
    public ShipmentResult execute(CreateShipmentCommand command) {
        Shipment draft = Shipment.createDraft(command.organizationId(), command.reference(),
                command.originSiteId(), command.destinationSiteId(), command.consigneeOrganizationId());
        List<ShipmentLine> lines = command.lines().stream().map(line -> toLine(draft, line)).toList();
        Shipment saved = shipments.save(draft.withLines(lines));
        participants.save(ShipmentParticipant.createNew(saved.id(), command.organizationId(),
                Participation.SHIPPER));
        participants.save(ShipmentParticipant.createNew(saved.id(), command.consigneeOrganizationId(),
                Participation.CONSIGNEE));
        custodyLog.append(saved.id(), CustodyEventKind.CREATED, null, command.organizationId(),
                command.originSiteId(), currentActor.requireId(), clock.instant());
        return mapper.toResult(saved);
    }

    private ShipmentLine toLine(Shipment shipment, ShipmentLineCommand line) {
        ProductResult product = catalog.productOf(line.productId());
        StorageProfileResult profile = catalog.activeProfileOf(line.productId());
        return ShipmentLine.createNew(shipment.id(), product.id(), product.name(), profile.version(),
                line.quantity(), line.unit());
    }
}
