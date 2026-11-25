package com.coldchain.triage.internal.domain.service;

import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import com.coldchain.triage.internal.domain.model.ExcursionContext;
import com.coldchain.triage.internal.domain.model.Triage;
import java.util.Optional;

/**
 * Asks a language model what probably caused a closed excursion (ADR-012). Empty whenever the model
 * gave no usable answer: none configured, no reply in time, a refusal, or an answer outside the schema.
 * The caller answers with the rules then, so an empty result is never an error.
 */
public interface TriageAdvisor {

    Optional<Triage> adviseOn(ExcursionAlert alert, ExcursionContext context);
}
