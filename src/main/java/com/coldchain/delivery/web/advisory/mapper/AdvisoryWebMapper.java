package com.coldchain.delivery.web.advisory.mapper;

import com.coldchain.delivery.web.advisory.dto.ExcursionAlertResponse;
import com.coldchain.modules.advisory.api.dto.ExcursionAlertResult;
import java.util.List;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Three summaries are stored and one is served: the one in the language the caller asked for, English
 * when it asked for none of the three. Translating at read time would put a model on the request path.
 */
@Component
public class AdvisoryWebMapper {

    public List<ExcursionAlertResponse> toResponses(List<ExcursionAlertResult> alerts) {
        String language = LocaleContextHolder.getLocale().getLanguage();
        return alerts.stream().map(alert -> toResponse(alert, language)).toList();
    }

    private static ExcursionAlertResponse toResponse(ExcursionAlertResult alert, String language) {
        String summary = switch (language) {
            case "es" -> alert.summaryEs();
            case "pt" -> alert.summaryPt();
            default -> alert.summaryEn();
        };
        return new ExcursionAlertResponse(alert.id(), alert.excursionId(), alert.probableCause(), alert.confidence(),
                alert.source(), alert.severity(), summary, alert.recommendedAction(), alert.model(),
                alert.contextAvailable(), alert.triagedAt());
    }
}
