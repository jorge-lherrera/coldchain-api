package com.coldchain.modules.advisory.internal.application.mapper;

import com.coldchain.modules.advisory.api.dto.ExcursionAlertResult;
import com.coldchain.modules.advisory.api.dto.RecordAlertCommand;
import com.coldchain.modules.advisory.internal.domain.model.Advice;
import com.coldchain.modules.advisory.internal.domain.model.ExcursionAlert;
import org.springframework.stereotype.Component;

@Component
public class AdvisoryApiMapper {

    public Advice toAdvice(RecordAlertCommand command) {
        return new Advice(command.probableCause(), command.severity(), command.confidence(), command.summaryEn(),
                command.summaryEs(), command.summaryPt(), command.recommendedAction(), command.source(),
                command.model(), command.contextAvailable(), command.triagedAt());
    }

    public ExcursionAlertResult toResult(ExcursionAlert alert) {
        Advice advice = alert.advice();
        return new ExcursionAlertResult(alert.id(), alert.shipmentId(), alert.excursionId(), advice.probableCause(),
                advice.severity(), advice.confidence(), advice.summaryEn(), advice.summaryEs(), advice.summaryPt(),
                advice.recommendedAction(), advice.source(), advice.model(), advice.contextAvailable(),
                advice.triagedAt());
    }
}
