package com.coldchain.modules.compliance.api.dto;

import com.coldchain.modules.compliance.api.FindingCode;
import com.coldchain.modules.compliance.api.Severity;
import java.util.UUID;

public record FindingResult(UUID id, FindingCode code, Severity severity, UUID excursionId,
        String detail) {
}
