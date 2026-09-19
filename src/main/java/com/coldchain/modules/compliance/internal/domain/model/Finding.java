package com.coldchain.modules.compliance.internal.domain.model;

import com.coldchain.modules.compliance.api.FindingCode;
import com.coldchain.modules.compliance.api.Severity;
import com.coldchain.shared.util.UuidV7;
import java.util.Objects;
import java.util.UUID;

public final class Finding {

    private final UUID id;

    private final UUID certificateId;

    private final FindingCode code;

    private final Severity severity;

    private final UUID excursionId;

    private final String detail;

    private Finding(UUID id, UUID certificateId, FindingCode code, Severity severity, UUID excursionId,
            String detail) {
        this.id = Objects.requireNonNull(id);
        this.certificateId = Objects.requireNonNull(certificateId);
        this.code = Objects.requireNonNull(code);
        this.severity = Objects.requireNonNull(severity);
        this.excursionId = excursionId;
        this.detail = detail;
    }

    public static Finding createNew(UUID certificateId, FindingCode code, Severity severity,
            UUID excursionId, String detail) {
        return new Finding(UuidV7.generate(), certificateId, code, severity, excursionId, detail);
    }

    public static Finding restore(UUID id, UUID certificateId, FindingCode code, Severity severity,
            UUID excursionId, String detail) {
        return new Finding(id, certificateId, code, severity, excursionId, detail);
    }

    public UUID id() {
        return id;
    }

    public UUID certificateId() {
        return certificateId;
    }

    public FindingCode code() {
        return code;
    }

    public Severity severity() {
        return severity;
    }

    public UUID excursionId() {
        return excursionId;
    }

    public String detail() {
        return detail;
    }
}
