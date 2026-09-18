package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.modules.catalog.api.CatalogApi;
import com.coldchain.modules.catalog.api.SiteKind;
import com.coldchain.modules.catalog.api.dto.CreateProductCommand;
import com.coldchain.modules.catalog.api.dto.CreateSiteCommand;
import com.coldchain.modules.catalog.api.dto.CreateStorageProfileCommand;
import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.catalog.api.dto.StorageThresholds;
import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.modules.identity.api.OrganizationKind;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.shipment.api.ShipmentApi;
import com.coldchain.modules.shipment.api.ShipmentStatus;
import com.coldchain.modules.shipment.api.dto.AcceptHandoffCommand;
import com.coldchain.modules.shipment.api.dto.CreateShipmentCommand;
import com.coldchain.modules.shipment.api.dto.DispatchShipmentCommand;
import com.coldchain.modules.shipment.api.dto.OpenHandoffCommand;
import com.coldchain.modules.shipment.api.dto.OpenHandoffResult;
import com.coldchain.modules.shipment.api.dto.ShipmentLineCommand;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.api.dto.ShipmentTimelineResult;
import com.coldchain.shared.error.DomainException;
import com.coldchain.shared.identifier.RawUuid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class ShipmentCustodyIT {

    private static final StorageThresholds FRIDGE = new StorageThresholds(
            new BigDecimal("2.00"), new BigDecimal("8.00"), 30, 120, new BigDecimal("95.00"));

    @Autowired
    private ShipmentApi shipments;

    @Autowired
    private CatalogApi catalog;

    @Autowired
    private IdentityApi identity;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void custodyMovesOnlyWhenTheCodeIsAccepted() {
        RegisterOrganizationResult shipper = organization("shipper");
        RegisterOrganizationResult carrier = organization("carrier");
        RegisterOrganizationResult consignee = organization("consignee");
        ShipmentResult dispatched = dispatchedShipment(shipper, consignee);

        OpenHandoffResult handoff = ActingAs.user(shipper.administratorId(), shipper.organizationId(),
                () -> shipments.openHandoff(
                        new OpenHandoffCommand(dispatched.id(), carrier.organizationId())));

        assertThat(ActingAs.user(shipper.administratorId(), shipper.organizationId(),
                () -> shipments.shipmentOf(dispatched.id(), shipper.organizationId()))
                .currentCustodianOrganizationId())
                .describedAs("opening a handoff moves nothing: only accepting it does")
                .isEqualTo(shipper.organizationId());

        ShipmentResult accepted = ActingAs.user(carrier.administratorId(), carrier.organizationId(),
                () -> shipments.acceptHandoff(new AcceptHandoffCommand(dispatched.id(),
                        carrier.organizationId(), handoff.code())));

        assertThat(accepted.currentCustodianOrganizationId()).isEqualTo(carrier.organizationId());
    }

    @Test
    void aWrongCodeAndAReusedCodeAreBothRefused() {
        RegisterOrganizationResult shipper = organization("shipper");
        RegisterOrganizationResult carrier = organization("carrier");
        RegisterOrganizationResult consignee = organization("consignee");
        ShipmentResult dispatched = dispatchedShipment(shipper, consignee);
        OpenHandoffResult handoff = ActingAs.user(shipper.administratorId(), shipper.organizationId(),
                () -> shipments.openHandoff(
                        new OpenHandoffCommand(dispatched.id(), carrier.organizationId())));

        assertThatThrownBy(() -> ActingAs.user(carrier.administratorId(), carrier.organizationId(),
                () -> shipments.acceptHandoff(new AcceptHandoffCommand(dispatched.id(),
                        carrier.organizationId(), "WRONGCOD"))))
                .isInstanceOf(DomainException.class)
                .extracting(failure -> ((DomainException) failure).errorCode().name())
                .isEqualTo("HANDOFF_CODE_INVALID");

        ActingAs.user(carrier.administratorId(), carrier.organizationId(),
                () -> shipments.acceptHandoff(new AcceptHandoffCommand(dispatched.id(),
                        carrier.organizationId(), handoff.code())));

        assertThatThrownBy(() -> ActingAs.user(carrier.administratorId(), carrier.organizationId(),
                () -> shipments.acceptHandoff(new AcceptHandoffCommand(dispatched.id(),
                        carrier.organizationId(), handoff.code()))))
                .describedAs("a code is single use, and there is no second pending request to find")
                .isInstanceOf(DomainException.class)
                .extracting(failure -> ((DomainException) failure).errorCode().name())
                .isEqualTo("HANDOFF_NOT_FOUND");
    }

    @Test
    void onlyOneHandoffCanBePendingOnAShipment() {
        RegisterOrganizationResult shipper = organization("shipper");
        RegisterOrganizationResult carrier = organization("carrier");
        RegisterOrganizationResult consignee = organization("consignee");
        ShipmentResult dispatched = dispatchedShipment(shipper, consignee);
        ActingAs.user(shipper.administratorId(), shipper.organizationId(),
                () -> shipments.openHandoff(
                        new OpenHandoffCommand(dispatched.id(), carrier.organizationId())));

        assertThatThrownBy(() -> ActingAs.user(shipper.administratorId(), shipper.organizationId(),
                () -> shipments.openHandoff(
                        new OpenHandoffCommand(dispatched.id(), consignee.organizationId()))))
                .describedAs("the database is what stops a second pending handoff, not a service check")
                .isInstanceOf(DomainException.class)
                .extracting(failure -> ((DomainException) failure).errorCode().name())
                .isEqualTo("HANDOFF_ALREADY_PENDING");
    }

    @Test
    void anOrganizationThatDoesNotTakePartGetsNothing() {
        RegisterOrganizationResult shipper = organization("shipper");
        RegisterOrganizationResult consignee = organization("consignee");
        RegisterOrganizationResult stranger = organization("stranger");
        ShipmentResult dispatched = dispatchedShipment(shipper, consignee);

        assertThatThrownBy(() -> shipments.shipmentOf(dispatched.id(), stranger.organizationId()))
                .describedAs("a 403 would confirm the shipment exists, which already leaks")
                .isInstanceOf(DomainException.class)
                .extracting(failure -> ((DomainException) failure).errorCode().name())
                .isEqualTo("SHIPMENT_NOT_FOUND");
        assertThat(shipments.listShipments(stranger.organizationId(), Pages.BY_REFERENCE).content()).isEmpty();
        assertThat(shipments.listShipments(consignee.organizationId(), Pages.BY_REFERENCE).content())
                .describedAs("the consignee takes part from the moment the shipment is created")
                .hasSize(1);
    }

    @Test
    void theChainVerifiesUntilSomebodyEditsIt() {
        RegisterOrganizationResult shipper = organization("shipper");
        RegisterOrganizationResult consignee = organization("consignee");
        ShipmentResult dispatched = dispatchedShipment(shipper, consignee);

        ShipmentTimelineResult timeline = shipments.timelineOf(dispatched.id(),
                shipper.organizationId());

        assertThat(timeline.events()).hasSize(2);
        assertThat(timeline.verdict().intact()).isTrue();

        jdbc.update("UPDATE custody_event SET kind = 'COMPENSATION' WHERE shipment_id = ? "
                + "AND sequence_number = 2", RawUuid.toBytes(dispatched.id()));

        ShipmentTimelineResult tampered = shipments.timelineOf(dispatched.id(),
                shipper.organizationId());

        assertThat(tampered.verdict().intact())
                .describedAs("a row edited straight in the database stops matching its own hash")
                .isFalse();
        assertThat(tampered.verdict().firstBrokenSequence()).isEqualTo(2);
    }

    @Test
    void aShipmentWithNoLinesCannotBeDispatched() {
        RegisterOrganizationResult shipper = organization("shipper");
        RegisterOrganizationResult consignee = organization("consignee");
        UUID site = ActingAs.user(shipper.administratorId(), shipper.organizationId(),
                () -> catalog.createSite(new CreateSiteCommand(shipper.organizationId(), code("SITE"),
                        "Origin", SiteKind.ORIGIN, new BigDecimal("-34.90"), new BigDecimal("-56.16"),
                        "America/Montevideo"))).id();

        ShipmentResult empty = ActingAs.user(shipper.administratorId(), shipper.organizationId(),
                () -> shipments.createShipment(new CreateShipmentCommand(shipper.organizationId(),
                        code("SHP"), site, site, consignee.organizationId(), List.of())));

        assertThatThrownBy(() -> ActingAs.user(shipper.administratorId(), shipper.organizationId(),
                () -> shipments.dispatchShipment(
                        new DispatchShipmentCommand(empty.id(), UUID.randomUUID()))))
                .describedAs("a draft may be empty; a dispatch may not")
                .isInstanceOf(DomainException.class)
                .extracting(failure -> ((DomainException) failure).errorCode().name())
                .isEqualTo("NO_LINES");
    }

    private ShipmentResult dispatchedShipment(RegisterOrganizationResult shipper,
            RegisterOrganizationResult consignee) {
        return ActingAs.user(shipper.administratorId(), shipper.organizationId(), () -> {
            UUID origin = catalog.createSite(new CreateSiteCommand(shipper.organizationId(),
                    code("SITE"), "Origin", SiteKind.ORIGIN, new BigDecimal("-34.90"),
                    new BigDecimal("-56.16"), "America/Montevideo")).id();
            UUID destination = catalog.createSite(new CreateSiteCommand(shipper.organizationId(),
                    code("SITE"), "Destination", SiteKind.DESTINATION, new BigDecimal("-23.55"),
                    new BigDecimal("-46.63"), "America/Sao_Paulo")).id();
            StorageProfileResult profile = catalog.createStorageProfile(
                    new CreateStorageProfileCommand(shipper.organizationId(), code("PROF"),
                            "Fridge 2-8", FRIDGE));
            catalog.activateStorageProfile(profile.id());
            UUID product = catalog.createProduct(new CreateProductCommand(shipper.organizationId(),
                    code("SKU"), "Vaccine", profile.id())).id();
            ShipmentResult draft = shipments.createShipment(new CreateShipmentCommand(
                    shipper.organizationId(), code("SHP"), origin, destination,
                    consignee.organizationId(),
                    List.of(new ShipmentLineCommand(product, new BigDecimal("10.000"), "BOX"))));
            ShipmentResult moved = shipments.dispatchShipment(
                    new DispatchShipmentCommand(draft.id(), UUID.randomUUID()));
            assertThat(moved.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);
            assertThat(moved.thresholds().minCelsius()).isEqualByComparingTo("2.00");
            return moved;
        });
    }

    private RegisterOrganizationResult organization(String who) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return identity.registerOrganization(new RegisterOrganizationCommand(
                "TAX-" + suffix, who + " Logistics S.A.", who, OrganizationKind.CARRIER, "UY",
                who + "-" + suffix + "@shipment.test", who + " Admin", "Shipment!Secret42"));
    }

    private static String code(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
