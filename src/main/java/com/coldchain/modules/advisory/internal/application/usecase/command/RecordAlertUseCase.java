package com.coldchain.modules.advisory.internal.application.usecase.command;

import com.coldchain.modules.advisory.api.dto.ExcursionAlertResult;
import com.coldchain.modules.advisory.api.dto.RecordAlertCommand;
import com.coldchain.modules.advisory.internal.application.mapper.AdvisoryApiMapper;
import com.coldchain.modules.advisory.internal.domain.model.Advice;
import com.coldchain.modules.advisory.internal.domain.model.ExcursionAlert;
import com.coldchain.modules.advisory.internal.domain.repository.ExcursionAlertRepository;
import com.coldchain.shared.annotation.UseCase;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RecordAlertUseCase {

    private final ExcursionAlertRepository alerts;

    private final AdvisoryApiMapper mapper;

    public RecordAlertUseCase(ExcursionAlertRepository alerts, AdvisoryApiMapper mapper) {
        this.alerts = alerts;
        this.mapper = mapper;
    }

    @Transactional
    public ExcursionAlertResult execute(RecordAlertCommand command) {
        Advice advice = mapper.toAdvice(command);
        ExcursionAlert alert = alerts.findByExcursionId(command.excursionId())
                .map(stored -> stored.outdatedBy(advice) ? alerts.save(stored.reconsideredWith(advice)) : stored)
                .orElseGet(() -> alerts.save(ExcursionAlert.createNew(command.organizationId(), command.shipmentId(),
                        command.excursionId(), advice)));
        return mapper.toResult(alert);
    }
}
