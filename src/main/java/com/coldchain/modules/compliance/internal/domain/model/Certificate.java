package com.coldchain.modules.compliance.internal.domain.model;

import com.coldchain.modules.compliance.api.Verdict;
import com.coldchain.shared.identifier.UuidV7;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Certificate {

    private final UUID id;

    private final UUID organizationId;

    private final UUID shipmentId;

    private final int version;

    private final Verdict verdict;

    private final BigDecimal coveragePercent;

    private final long cumulativeExcursionMinutes;

    private final long longestExcursionMinutes;

    private final String thresholdSnapshot;

    private final Instant evaluatedFrom;

    private final Instant evaluatedTo;

    private final Instant issuedAt;

    private final String contentHash;

    private final Instant supersededAt;

    private final List<Finding> findings;

    private Certificate(UUID id, UUID organizationId, UUID shipmentId, int version,
            Verdict verdict, BigDecimal coveragePercent, long cumulativeExcursionMinutes,
            long longestExcursionMinutes, String thresholdSnapshot, Instant evaluatedFrom,
            Instant evaluatedTo, Instant issuedAt, String contentHash, Instant supersededAt,
            List<Finding> findings) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.shipmentId = Objects.requireNonNull(shipmentId);
        this.version = version;
        this.verdict = Objects.requireNonNull(verdict);
        this.coveragePercent = Objects.requireNonNull(coveragePercent);
        this.cumulativeExcursionMinutes = cumulativeExcursionMinutes;
        this.longestExcursionMinutes = longestExcursionMinutes;
        this.thresholdSnapshot = Objects.requireNonNull(thresholdSnapshot);
        this.evaluatedFrom = Objects.requireNonNull(evaluatedFrom);
        this.evaluatedTo = Objects.requireNonNull(evaluatedTo);
        this.issuedAt = Objects.requireNonNull(issuedAt);
        this.contentHash = Objects.requireNonNull(contentHash);
        this.supersededAt = supersededAt;
        this.findings = List.copyOf(findings);
        if (verdict != Verdict.PASS && findings.isEmpty()) {
            throw new IllegalArgumentException(
                    "A verdict that is not a pass must say what it found, or it argues nothing");
        }
    }

    public static Certificate issue(UUID organizationId, UUID shipmentId, int version,
            Verdict verdict, BigDecimal coveragePercent, long cumulativeExcursionMinutes,
            long longestExcursionMinutes, String thresholdSnapshot, Instant evaluatedFrom,
            Instant evaluatedTo, Instant issuedAt, String contentHash, List<Finding> findings) {
        return new Certificate(UuidV7.generate(), organizationId, shipmentId, version,
                verdict, coveragePercent, cumulativeExcursionMinutes, longestExcursionMinutes,
                thresholdSnapshot, evaluatedFrom, evaluatedTo, issuedAt, contentHash, null, findings);
    }

    public static Certificate restore(UUID id, UUID organizationId, UUID shipmentId,
            int version, Verdict verdict, BigDecimal coveragePercent, long cumulativeExcursionMinutes,
            long longestExcursionMinutes, String thresholdSnapshot, Instant evaluatedFrom,
            Instant evaluatedTo, Instant issuedAt, String contentHash, Instant supersededAt,
            List<Finding> findings) {
        return new Certificate(id, organizationId, shipmentId, version, verdict,
                coveragePercent, cumulativeExcursionMinutes, longestExcursionMinutes,
                thresholdSnapshot, evaluatedFrom, evaluatedTo, issuedAt, contentHash, supersededAt,
                findings);
    }

    public Certificate supersede(Instant when) {
        return new Certificate(id, organizationId, shipmentId, version, verdict,
                coveragePercent, cumulativeExcursionMinutes, longestExcursionMinutes,
                thresholdSnapshot, evaluatedFrom, evaluatedTo, issuedAt, contentHash, when, findings);
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID shipmentId() {
        return shipmentId;
    }

    public int version() {
        return version;
    }

    public Verdict verdict() {
        return verdict;
    }

    public BigDecimal coveragePercent() {
        return coveragePercent;
    }

    public long cumulativeExcursionMinutes() {
        return cumulativeExcursionMinutes;
    }

    public long longestExcursionMinutes() {
        return longestExcursionMinutes;
    }

    public String thresholdSnapshot() {
        return thresholdSnapshot;
    }

    public Instant evaluatedFrom() {
        return evaluatedFrom;
    }

    public Instant evaluatedTo() {
        return evaluatedTo;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public String contentHash() {
        return contentHash;
    }

    public Instant supersededAt() {
        return supersededAt;
    }

    public List<Finding> findings() {
        return findings;
    }
}
