package com.coldchain.triage.internal.application;

import com.coldchain.triage.api.TriageApi;
import com.coldchain.triage.api.dto.RecordExcursionEventCommand;
import com.coldchain.triage.api.dto.RecordExcursionEventResult;
import com.coldchain.triage.internal.application.usecase.command.RecordExcursionEventUseCase;
import org.springframework.stereotype.Component;

@Component
public class TriageFacade implements TriageApi {

    private final RecordExcursionEventUseCase recordExcursionEvent;

    public TriageFacade(RecordExcursionEventUseCase recordExcursionEvent) {
        this.recordExcursionEvent = recordExcursionEvent;
    }

    @Override
    public RecordExcursionEventResult recordExcursionEvent(RecordExcursionEventCommand command) {
        return recordExcursionEvent.execute(command);
    }
}
