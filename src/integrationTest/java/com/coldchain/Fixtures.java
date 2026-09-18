package com.coldchain;

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
import com.coldchain.modules.shipment.api.dto.CreateShipmentCommand;
import com.coldchain.modules.shipment.api.dto.DispatchShipmentCommand;
import com.coldchain.modules.shipment.api.dto.ShipmentLineCommand;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.telemetry.api.TelemetryApi;
import com.coldchain.modules.telemetry.api.dto.AssignDeviceCommand;
import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.MonitoringThresholds;
import com.coldchain.modules.telemetry.api.dto.RegisterDeviceCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class Fixtures {

    static final StorageThresholds FRIDGE = new StorageThresholds(
            new BigDecimal("2.00"), new BigDecimal("8.00"), 30, 120, new BigDecimal("80.00"));

    static final Instant START = Instant.parse("2026-04-01T08:00:00Z");

    @Autowired
    private IdentityApi identity;

    @Autowired
    private CatalogApi catalog;

    @Autowired
    private ShipmentApi shipments;

    @Autowired
    private TelemetryApi telemetry;

    RegisterOrganizationResult organization(String name) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return identity.registerOrganization(new RegisterOrganizationCommand(
                "TAX-" + suffix, name + " S.A.", name, OrganizationKind.SHIPPER, "UY",
                name + "-" + suffix + "@fixtures.test", name + " Admin", "Fixture!Secret42"));
    }

    DeviceResult device(RegisterOrganizationResult owner) {
        return telemetry.registerDevice(new RegisterDeviceCommand(owner.organizationId(),
                "SN-" + UUID.randomUUID().toString().substring(0, 8), "Tag-1", "1.4.2", 300,
                START.minus(30, ChronoUnit.DAYS)));
    }

    ShipmentResult draftShipment(RegisterOrganizationResult owner) {
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

    ShipmentResult dispatchedShipment(RegisterOrganizationResult owner, DeviceResult device) {
        ShipmentResult draft = draftShipment(owner);
        ShipmentResult dispatched = ActingAs.user(owner.administratorId(), owner.organizationId(),
                () -> shipments.dispatchShipment(new DispatchShipmentCommand(draft.id(), device.id())));
        telemetry.assignDevice(new AssignDeviceCommand(device.id(), dispatched.id(),
                new MonitoringThresholds(new BigDecimal("2.00"), new BigDecimal("8.00")),
                dispatched.dispatchedAt()));
        return dispatched;
    }

    static String code(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
