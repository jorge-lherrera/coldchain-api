package com.coldchain.modules.advisory.api;

import com.coldchain.modules.advisory.api.dto.ExcursionAlertResult;
import com.coldchain.modules.advisory.api.dto.RecordAlertCommand;
import java.util.List;
import java.util.UUID;

public interface AdvisoryApi {

    ExcursionAlertResult recordAlert(RecordAlertCommand command);

    List<ExcursionAlertResult> alertsOf(UUID shipmentId, UUID viewerOrganizationId);
}
