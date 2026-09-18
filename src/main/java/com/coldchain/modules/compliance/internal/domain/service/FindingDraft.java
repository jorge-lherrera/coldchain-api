package com.coldchain.modules.compliance.internal.domain.service;

import com.coldchain.modules.compliance.api.FindingCode;
import com.coldchain.modules.compliance.api.Severity;
import java.util.UUID;

public record FindingDraft(FindingCode code, Severity severity, UUID excursionId, String detail) {
}
