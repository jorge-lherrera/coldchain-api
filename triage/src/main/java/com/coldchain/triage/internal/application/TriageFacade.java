package com.coldchain.triage.internal.application;

import com.coldchain.triage.api.TriageApi;
import com.coldchain.triage.api.dto.RecordExcursionEventCommand;
import com.coldchain.triage.api.dto.RecordExcursionEventResult;
import com.coldchain.triage.internal.application.usecase.command.RecordExcursionEventUseCase;
import com.coldchain.triage.internal.application.usecase.command.TriageExcursionUseCase;
import org.springframework.stereotype.Component;

@Component
public class TriageFacade implements TriageApi {

    private final RecordExcursionEventUseCase recordExcursionEvent;

    private final TriageExcursionUseCase triageExcursion;

    public TriageFacade(RecordExcursionEventUseCase recordExcursionEvent,
            TriageExcursionUseCase triageExcursion) {
        this.recordExcursionEvent = recordExcursionEvent;
        this.triageExcursion = triageExcursion;
    }

    @Override
    public RecordExcursionEventResult recordExcursionEvent(RecordExcursionEventCommand command) {
        RecordExcursionEventResult recorded = recordExcursionEvent.execute(command);
        if (recorded.needsContext()) {
            triageExcursion.execute(recorded.eventId(), recorded.excursionId());
        }
        return recorded;
    }
}
