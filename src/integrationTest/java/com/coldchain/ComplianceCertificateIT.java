package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.catalog.api.CatalogApi;
import com.coldchain.modules.catalog.api.SiteKind;
import com.coldchain.modules.catalog.api.dto.CreateProductCommand;
import com.coldchain.modules.catalog.api.dto.CreateSiteCommand;
import com.coldchain.modules.catalog.api.dto.CreateStorageProfileCommand;
import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.catalog.api.dto.ThresholdsView;
import com.coldchain.modules.compliance.api.ComplianceApi;
import com.coldchain.modules.compliance.api.FindingCode;
import com.coldchain.modules.compliance.api.Verdict;
import com.coldchain.modules.compliance.api.dto.CertificateResult;
import com.coldchain.modules.compliance.api.dto.FindingResult;
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
import com.coldchain.modules.telemetry.api.dto.IngestBatchCommand;
import com.coldchain.modules.telemetry.api.dto.MonitoringThresholds;
import com.coldchain.modules.telemetry.api.dto.ReadingCommand;
import com.coldchain.modules.telemetry.api.dto.RegisterDeviceCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ComplianceCertificateIT {

    private static final ThresholdsView FRIDGE = new ThresholdsView(
            new BigDecimal("2.00"), new BigDecimal("8.00"), 30, 120, new BigDecimal("80.00"));

    @Autowired
    private ComplianceApi compliance;

    @Autowired
    private ShipmentApi shipments;

    @Autowired
    private TelemetryApi telemetry;

    @Autowired
    private CatalogApi catalog;

    @Autowired
    private IdentityApi identity;

    @Test
    void aSeriesWithinBandAndWellCoveredPasses() {
        RegisterOrganizationResult organization = organization();
        DeviceResult device = device(organization);
        ShipmentResult shipment = dispatched(organization, device);
        monitor(organization, device, shipment);
        ingest(organization, device, steady(shipment.dispatchedAt()));

        CertificateResult certificate = ActingAs.user(organization.administratorId(),
                organization.organizationId(),
                () -> compliance.issueCertificate(shipment.id(), organization.organizationId()));

        assertThat(certificate.verdict()).isEqualTo(Verdict.PASS);
        assertThat(certificate.version()).isEqualTo(1);
        assertThat(certificate.findings()).isEmpty();
        assertThat(certificate.contentHash()).hasSize(64);
    }

    @Test
    void aLongExcursionFailsAndSaysWhy() {
        RegisterOrganizationResult organization = organization();
        DeviceResult device = device(organization);
        ShipmentResult shipment = dispatched(organization, device);
        monitor(organization, device, shipment);
        ingest(organization, device, withLongExcursion(shipment.dispatchedAt()));

        CertificateResult certificate = ActingAs.user(organization.administratorId(),
                organization.organizationId(),
                () -> compliance.issueCertificate(shipment.id(), organization.organizationId()));

        assertThat(certificate.verdict()).isEqualTo(Verdict.FAIL);
        assertThat(certificate.findings()).extracting(FindingResult::code)
                .describedAs("a verdict that cannot say why is no use for arguing with anyone")
                .contains(FindingCode.EXCURSION_ABOVE_MAX);
        assertThat(certificate.longestExcursionMinutes()).isGreaterThan(30);
    }

    @Test
    void noReadingsMeansNoPassEvenWithNoExcursions() {
        RegisterOrganizationResult organization = organization();
        DeviceResult device = device(organization);
        ShipmentResult shipment = dispatched(organization, device);
        monitor(organization, device, shipment);

        CertificateResult certificate = ActingAs.user(organization.administratorId(),
                organization.organizationId(),
                () -> compliance.issueCertificate(shipment.id(), organization.organizationId()));

        assertThat(certificate.verdict())
                .describedAs("not measuring is not complying: turning the sensor off cannot be a pass")
                .isEqualTo(Verdict.FAIL);
        assertThat(certificate.findings()).extracting(FindingResult::code)
                .contains(FindingCode.DATA_GAP);
        assertThat(certificate.coveragePercent()).isEqualByComparingTo("0.00");
    }

    @Test
    void issuingAgainSupersedesTheOneBefore() {
        RegisterOrganizationResult organization = organization();
        DeviceResult device = device(organization);
        ShipmentResult shipment = dispatched(organization, device);
        monitor(organization, device, shipment);
        ingest(organization, device, steady(shipment.dispatchedAt()));

        CertificateResult first = ActingAs.user(organization.administratorId(),
                organization.organizationId(),
                () -> compliance.issueCertificate(shipment.id(), organization.organizationId()));
        CertificateResult second = ActingAs.user(organization.administratorId(),
                organization.organizationId(),
                () -> compliance.issueCertificate(shipment.id(), organization.organizationId()));

        assertThat(second.version()).isEqualTo(first.version() + 1);
        assertThat(compliance.currentCertificateOf(shipment.id(), organization.organizationId()).id())
                .describedAs("the previous version stays queryable but is no longer the current one")
                .isEqualTo(second.id());
    }

    private List<ReadingCommand> steady(Instant from) {
        List<ReadingCommand> readings = new ArrayList<>();
        for (int minute = 0; minute <= 60; minute += 5) {
            readings.add(new ReadingCommand(from.plus(minute, ChronoUnit.MINUTES),
                    new BigDecimal("4.50")));
        }
        return readings;
    }

    private List<ReadingCommand> withLongExcursion(Instant from) {
        List<ReadingCommand> readings = new ArrayList<>();
        for (int minute = 0; minute <= 60; minute += 5) {
            String celsius = minute >= 10 && minute <= 50 ? "11.00" : "4.50";
            readings.add(new ReadingCommand(from.plus(minute, ChronoUnit.MINUTES),
                    new BigDecimal(celsius)));
        }
        return readings;
    }

    private Instant start() {
        return Instant.parse("2026-04-01T08:00:00Z");
    }

    private void ingest(RegisterOrganizationResult organization, DeviceResult device,
            List<ReadingCommand> readings) {
        telemetry.ingestBatch(new IngestBatchCommand(organization.organizationId(), device.id(),
                "batch-" + UUID.randomUUID(), readings));
    }

    private DeviceResult device(RegisterOrganizationResult organization) {
        return telemetry.registerDevice(new RegisterDeviceCommand(organization.organizationId(),
                "SN-" + UUID.randomUUID().toString().substring(0, 8), "Tag-1", "1.4.2", 300,
                start().minus(30, ChronoUnit.DAYS)));
    }

    private void monitor(RegisterOrganizationResult organization, DeviceResult device,
            ShipmentResult shipment) {
        telemetry.assignDevice(new AssignDeviceCommand(device.id(), shipment.id(),
                new MonitoringThresholds(new BigDecimal("2.00"), new BigDecimal("8.00")),
                shipment.dispatchedAt()));
    }

    private ShipmentResult dispatched(RegisterOrganizationResult organization, DeviceResult device) {
        return ActingAs.user(organization.administratorId(), organization.organizationId(), () -> {
            UUID origin = catalog.createSite(new CreateSiteCommand(organization.organizationId(),
                    code("SITE"), "Origin", SiteKind.ORIGIN, new BigDecimal("-34.90"),
                    new BigDecimal("-56.16"), "America/Montevideo")).id();
            StorageProfileResult profile = catalog.createStorageProfile(
                    new CreateStorageProfileCommand(organization.organizationId(), code("PROF"),
                            "Fridge 2-8", FRIDGE));
            catalog.activateStorageProfile(profile.id());
            UUID product = catalog.createProduct(new CreateProductCommand(
                    organization.organizationId(), code("SKU"), "Vaccine", profile.id())).id();
            ShipmentResult draft = shipments.createShipment(new CreateShipmentCommand(
                    organization.organizationId(), code("SHP"), origin, origin,
                    organization.organizationId(),
                    List.of(new ShipmentLineCommand(product, new BigDecimal("10.000"), "BOX"))));
            return shipments.dispatchShipment(
                    new DispatchShipmentCommand(draft.id(), device.id()));
        });
    }

    private RegisterOrganizationResult organization() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return identity.registerOrganization(new RegisterOrganizationCommand(
                "TAX-" + suffix, "Compliance Labs S.A.", "Compliance", OrganizationKind.LAB, "UY",
                "admin-" + suffix + "@compliance.test", "Compliance Admin", "Compliance!Secret42"));
    }

    private static String code(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
