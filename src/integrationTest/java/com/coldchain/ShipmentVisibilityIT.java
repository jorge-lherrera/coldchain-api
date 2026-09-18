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
import com.coldchain.modules.shipment.api.Participation;
import com.coldchain.modules.shipment.api.ShipmentApi;
import com.coldchain.modules.shipment.api.dto.AddParticipantCommand;
import com.coldchain.modules.shipment.api.dto.CreateShipmentCommand;
import com.coldchain.modules.shipment.api.dto.ShipmentLineCommand;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.shared.error.DomainException;
import com.coldchain.shared.error.ErrorCategory;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import com.coldchain.shared.paging.SortDirection;
import com.coldchain.shared.paging.SortOrder;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ShipmentVisibilityIT {

    private static final StorageThresholds FRIDGE = new StorageThresholds(
            new BigDecimal("2.00"), new BigDecimal("8.00"), 30, 120, new BigDecimal("80.00"));

    private static final PageCriteria FIRST_PAGE =
            new PageCriteria(0, 50, List.of(new SortOrder("reference", SortDirection.ASC)));

    @Autowired
    private ShipmentApi shipments;

    @Autowired
    private CatalogApi catalog;

    @Autowired
    private IdentityApi identity;

    @Test
    void aShipmentYouDoNotParticipateInDoesNotExistForYou() {
        RegisterOrganizationResult shipper = organization("shipper");
        RegisterOrganizationResult stranger = organization("stranger");
        ShipmentResult shipment = shipmentOf(shipper);

        assertThatThrownBy(() -> shipments.shipmentOf(shipment.id(), stranger.organizationId()))
                .describedAs("a 403 confirms the resource exists, and that is already a leak")
                .isInstanceOf(DomainException.class)
                .extracting(failure -> ((DomainException) failure).errorCode().category())
                .isEqualTo(ErrorCategory.NOT_FOUND);
    }

    @Test
    void aListingShowsOnlyWhatTheViewerTakesPartIn() {
        RegisterOrganizationResult shipper = organization("shipper");
        RegisterOrganizationResult stranger = organization("stranger");
        ShipmentResult mine = shipmentOf(shipper);

        PagedResult<ShipmentResult> seen =
                shipments.listShipments(stranger.organizationId(), FIRST_PAGE);

        assertThat(seen.content()).extracting(ShipmentResult::id)
                .describedAs("participation is what makes a shipment visible, in the listing too")
                .doesNotContain(mine.id());
        assertThat(shipments.listShipments(shipper.organizationId(), FIRST_PAGE).content())
                .extracting(ShipmentResult::id)
                .contains(mine.id());
    }

    @Test
    void addingSomebodyAsAParticipantIsWhatOpensTheDoor() {
        RegisterOrganizationResult shipper = organization("shipper");
        RegisterOrganizationResult carrier = organization("carrier");
        ShipmentResult shipment = shipmentOf(shipper);

        ActingAs.user(shipper.administratorId(), shipper.organizationId(),
                () -> shipments.addParticipant(new AddParticipantCommand(shipment.id(),
                        carrier.organizationId(), Participation.CARRIER)));

        assertThat(shipments.shipmentOf(shipment.id(), carrier.organizationId()).id())
                .describedAs("the same call that failed before now answers, and nothing else changed")
                .isEqualTo(shipment.id());
    }

    private ShipmentResult shipmentOf(RegisterOrganizationResult owner) {
        return ActingAs.user(owner.administratorId(), owner.organizationId(), () -> {
            UUID site = catalog.createSite(new CreateSiteCommand(owner.organizationId(), code("SITE"),
                    "Origin", SiteKind.ORIGIN, new BigDecimal("-34.90"), new BigDecimal("-56.16"),
                    "America/Montevideo")).id();
            StorageProfileResult profile = catalog.createStorageProfile(new CreateStorageProfileCommand(
                    owner.organizationId(), code("PROF"), "Fridge 2-8", FRIDGE));
            catalog.activateStorageProfile(profile.id());
            UUID product = catalog.createProduct(new CreateProductCommand(owner.organizationId(),
                    code("SKU"), "Vaccine", profile.id())).id();
            return shipments.createShipment(new CreateShipmentCommand(owner.organizationId(),
                    code("SHP"), site, site, owner.organizationId(),
                    List.of(new ShipmentLineCommand(product, new BigDecimal("10.000"), "BOX"))));
        });
    }

    private RegisterOrganizationResult organization(String name) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return identity.registerOrganization(new RegisterOrganizationCommand(
                "TAX-" + suffix, name + " S.A.", name, OrganizationKind.SHIPPER, "UY",
                name + "-" + suffix + "@visibility.test", name + " Admin", "Visible!Secret42"));
    }

    private static String code(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
