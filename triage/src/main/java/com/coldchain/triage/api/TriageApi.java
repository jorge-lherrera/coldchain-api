package com.coldchain.triage.api;

import com.coldchain.triage.api.dto.RecordExcursionEventCommand;
import com.coldchain.triage.api.dto.RecordExcursionEventResult;

public interface TriageApi {

    RecordExcursionEventResult recordExcursionEvent(RecordExcursionEventCommand command);
}
