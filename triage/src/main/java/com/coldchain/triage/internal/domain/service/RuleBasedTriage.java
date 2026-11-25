package com.coldchain.triage.internal.domain.service;

import com.coldchain.triage.api.ExcursionKind;
import com.coldchain.triage.api.ProbableCause;
import com.coldchain.triage.api.Severity;
import com.coldchain.triage.api.TriageSource;
import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import com.coldchain.triage.internal.domain.model.ExcursionContext;
import com.coldchain.triage.internal.domain.model.FrozenThresholds;
import com.coldchain.triage.internal.domain.model.Summary;
import com.coldchain.triage.internal.domain.model.Triage;
import com.coldchain.triage.internal.domain.model.TriageProvenance;
import java.math.BigDecimal;
import java.time.Instant;

public final class RuleBasedTriage {

    static final long DEFAULT_ALLOWED_MINUTES = 30;

    private static final String OPEN_ACTION =
            "Check the shipment now and confirm with whoever holds it that the cooling works.";

    private static final String CLOSED_ACTION =
            "Review the readings around the excursion with the holder of the shipment and record what caused it.";

    private RuleBasedTriage() {
    }

    public static Triage of(ExcursionAlert alert, Instant triagedAt) {
        return triage(alert, severityOf(alert, DEFAULT_ALLOWED_MINUTES), false, triagedAt);
    }

    public static Triage of(ExcursionAlert alert, ExcursionContext context, Instant triagedAt) {
        long allowed = context.frozenThresholds()
                .map(FrozenThresholds::maxSingleExcursionMinutes)
                .map(Integer::longValue)
                .orElse(DEFAULT_ALLOWED_MINUTES);
        return triage(alert, severityOf(alert, allowed), true, triagedAt);
    }

    private static Triage triage(ExcursionAlert alert, Severity severity, boolean contextAvailable,
            Instant triagedAt) {
        return new Triage(ProbableCause.UNKNOWN, severity, null, summaryOf(alert),
                alert.closed() ? CLOSED_ACTION : OPEN_ACTION, TriageSource.RULES,
                TriageProvenance.ofRules(contextAvailable), triagedAt);
    }

    static Severity severityOf(ExcursionAlert alert, long allowedMinutes) {
        if (!alert.closed() || alert.durationMinutes() == null) {
            return Severity.MEDIUM;
        }
        long minutes = alert.durationMinutes();
        if (minutes >= allowedMinutes) {
            return Severity.HIGH;
        }
        return minutes * 2 >= allowedMinutes ? Severity.MEDIUM : Severity.LOW;
    }

    private static Summary summaryOf(ExcursionAlert alert) {
        boolean above = alert.kind() == ExcursionKind.ABOVE_MAX;
        BigDecimal peak = alert.peakCelsius();
        if (!alert.closed()) {
            return new Summary(
                    (above ? "The temperature went above the maximum" : "The temperature fell below the minimum")
                            + " and is still out of range." + peakSentence(peak,
                            above ? " Highest reading so far: " : " Lowest reading so far: ", '.'),
                    (above ? "La temperatura superó el máximo" : "La temperatura bajó del mínimo")
                            + " y sigue fuera de rango." + peakSentence(peak,
                            above ? " Lectura más alta hasta ahora: " : " Lectura más baja hasta ahora: ", ','),
                    (above ? "A temperatura passou do máximo" : "A temperatura caiu abaixo do mínimo")
                            + " e continua fora da faixa." + peakSentence(peak,
                            above ? " Leitura mais alta até agora: " : " Leitura mais baixa até agora: ", ','));
        }
        long minutes = alert.durationMinutes() == null ? 0 : alert.durationMinutes();
        return new Summary(
                "The temperature stayed " + (above ? "above the maximum" : "below the minimum") + " for "
                        + minutes + (minutes == 1 ? " minute." : " minutes.")
                        + peakSentence(peak, above ? " Highest reading: " : " Lowest reading: ", '.'),
                "La temperatura estuvo " + (above ? "por encima del máximo" : "por debajo del mínimo")
                        + " durante " + minutes + (minutes == 1 ? " minuto." : " minutos.")
                        + peakSentence(peak, above ? " Lectura más alta: " : " Lectura más baja: ", ','),
                "A temperatura ficou " + (above ? "acima do máximo" : "abaixo do mínimo") + " por "
                        + minutes + (minutes == 1 ? " minuto." : " minutos.")
                        + peakSentence(peak, above ? " Leitura mais alta: " : " Leitura mais baixa: ", ','));
    }

    private static String peakSentence(BigDecimal peak, String lead, char decimalSeparator) {
        if (peak == null) {
            return "";
        }
        String celsius = peak.stripTrailingZeros().toPlainString().replace('.', decimalSeparator);
        return lead + celsius + " °C.";
    }
}
