package com.coldchain.modules.compliance.internal.application.usecase.command;

import com.coldchain.modules.compliance.api.dto.CertificateResult;
import com.coldchain.modules.compliance.internal.application.mapper.ComplianceApiMapper;
import com.coldchain.modules.compliance.internal.domain.model.Certificate;
import com.coldchain.modules.compliance.internal.domain.model.Finding;
import com.coldchain.modules.compliance.internal.domain.repository.CertificateRepository;
import com.coldchain.modules.compliance.internal.domain.service.Evaluation;
import com.coldchain.modules.compliance.internal.domain.service.EvaluationInput;
import com.coldchain.modules.compliance.internal.domain.service.ExcursionFact;
import com.coldchain.modules.compliance.internal.domain.service.VerdictCalculator;
import com.coldchain.modules.compliance.internal.exception.ComplianceErrorCode;
import com.coldchain.modules.shipment.api.ShipmentApi;
import com.coldchain.modules.shipment.api.ShipmentStatus;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.telemetry.api.ExcursionKind;
import com.coldchain.modules.telemetry.api.ExcursionStatus;
import com.coldchain.modules.telemetry.api.TelemetryApi;
import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.SeriesResult;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import com.coldchain.shared.identifier.UuidV7;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class IssueCertificateUseCase {

    private static final Duration CALIBRATION_VALIDITY = Duration.ofDays(365);

    private final CertificateRepository certificates;

    private final ShipmentApi shipments;

    private final TelemetryApi telemetry;

    private final ComplianceApiMapper mapper;

    private final Clock clock;

    public IssueCertificateUseCase(CertificateRepository certificates, ShipmentApi shipments,
            TelemetryApi telemetry, ComplianceApiMapper mapper, Clock clock) {
        this.certificates = certificates;
        this.shipments = shipments;
        this.telemetry = telemetry;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public CertificateResult execute(UUID shipmentId, UUID organizationId) {
        ShipmentResult shipment = shipments.shipmentOf(shipmentId, organizationId);
        if (shipment.status() == ShipmentStatus.DRAFT || shipment.dispatchedAt() == null) {
            throw DomainException.of(ComplianceErrorCode.SHIPMENT_NOT_READY);
        }
        if (shipment.thresholds() == null) {
            throw DomainException.of(ComplianceErrorCode.THRESHOLDS_NOT_FROZEN);
        }

        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Instant from = shipment.dispatchedAt();
        SeriesResult series = telemetry.seriesOf(shipmentId, organizationId);
        Instant to = evaluatedTo(shipment.closedAt() == null ? now : shipment.closedAt(), series);
        Evaluation evaluation = VerdictCalculator.evaluate(new EvaluationInput(
                shipment.thresholds().minCelsius(),
                shipment.thresholds().maxCelsius(),
                shipment.thresholds().maxSingleExcursionMinutes(),
                shipment.thresholds().maxCumulativeExcursionMinutes(),
                shipment.thresholds().minCoveragePercent(),
                from, to,
                samplingIntervalOf(shipment),
                series.points().size(),
                calibrationValid(shipment, to),
                shipment.deviceId() != null,
                factsOf(series)));

        certificates.findCurrent(shipmentId)
                .ifPresent(current -> certificates.save(current.supersede(now)));
        int version = certificates.highestVersionOf(shipmentId) + 1;
        String snapshot = mapper.snapshotOf(shipment.thresholds());
        UUID certificateId = UuidV7.generate();
        List<Finding> findings = new ArrayList<>();
        evaluation.findings().forEach(draft -> findings.add(Finding.createNew(certificateId,
                draft.code(), draft.severity(), draft.excursionId(), draft.detail())));

        Certificate issued = Certificate.issue(organizationId, shipmentId, version,
                evaluation.verdict(), evaluation.coveragePercent(),
                evaluation.cumulativeExcursionMinutes(), evaluation.longestExcursionMinutes(),
                snapshot, from, to, now,
                digestOf(shipmentId, version, evaluation.verdict().name(), snapshot, now), findings);
        return mapper.toResult(certificates.save(rebuild(issued, findings)));
    }

    private static Instant evaluatedTo(Instant closing, SeriesResult series) {
        return series.points().stream()
                .map(point -> point.measuredAt())
                .max(Instant::compareTo)
                .filter(last -> last.isAfter(closing))
                .orElse(closing);
    }

    private Certificate rebuild(Certificate issued, List<Finding> findings) {
        return Certificate.restore(issued.id(), issued.organizationId(), issued.shipmentId(),
                issued.version(), issued.verdict(), issued.coveragePercent(),
                issued.cumulativeExcursionMinutes(), issued.longestExcursionMinutes(),
                issued.thresholdSnapshot(), issued.evaluatedFrom(), issued.evaluatedTo(),
                issued.issuedAt(), issued.contentHash(), null,
                findings.stream()
                        .map(finding -> Finding.restore(finding.id(), issued.id(), finding.code(),
                                finding.severity(), finding.excursionId(), finding.detail()))
                        .toList());
    }

    private int samplingIntervalOf(ShipmentResult shipment) {
        if (shipment.deviceId() == null) {
            return 0;
        }
        return telemetry.deviceOf(shipment.deviceId()).samplingIntervalSeconds();
    }

    private boolean calibrationValid(ShipmentResult shipment, Instant until) {
        if (shipment.deviceId() == null) {
            return false;
        }
        DeviceResult device = telemetry.deviceOf(shipment.deviceId());
        return device.calibratedAt().plus(CALIBRATION_VALIDITY).isAfter(until);
    }

    private static List<ExcursionFact> factsOf(SeriesResult series) {
        return series.excursions().stream()
                .map(excursion -> new ExcursionFact(excursion.id(),
                        excursion.kind() == ExcursionKind.ABOVE_MAX,
                        excursion.status() == ExcursionStatus.CLOSED,
                        excursion.durationMinutes(), excursion.peakCelsius()))
                .toList();
    }

    private static String digestOf(UUID shipmentId, int version, String verdict, String snapshot,
            Instant issuedAt) {
        String material = String.join("|", shipmentId.toString(), Integer.toString(version), verdict,
                snapshot, issuedAt.toString());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException cause) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", cause);
        }
    }
}
