# ADR-012 — Excursion triage is advice, not evidence

- **Status:** Proposed
- **Date:** 2026-11-25

## Context

The certificate is the evidence this project exists to produce. It is computed from thresholds frozen
at dispatch ([ADR-004](ADR-004-frozen-thresholds.md)), it is immutable and hashed
([ADR-005](ADR-005-immutable-certificate.md)), and every finding points at the fact that produced it.
Anybody can recompute it and get the same answer.

A language model's triage ([ADR-010](ADR-010-second-service-for-excursion-triage.md)) is none of
that:

- **It is a probability, not a fact.** It can be wrong.
- **It changes** when the model or the prompt changes.
- **It cannot be recomputed** to the same bytes.

It is still worth having. Ten seconds after a vaccine goes above 8 °C, a carrier is better off with
"probably handled during the handoff, check the transfer dock" than with a number and a timestamp.

The risk is not that the model is wrong. The risk is a model's guess drifting, one convenient import
at a time, into the thing that is supposed to be evidence.

## Decision

**The triage is advice, kept in a module the verdict cannot reach, produced in a shape that can be
checked, with a deterministic answer whenever the model does not give one.**

- **The verdict cannot reach the advice, and the build proves it.**
  - The alert lives in a new API module, `advisory`, and `compliance` does not list it in
    `allowedDependencies`. If the verdict ever reads an alert, the build fails: Spring Modulith
    verifies it, and a row in the rule catalogue names the test.
  - `advisory` depends on `shipment::api` only, to resolve visibility. Nothing depends on
    `advisory`.
- **Structured output, never free text.** The model answers a schema:
  - `probableCause`, a closed set: `HANDLING_DURING_HANDOFF`, `REFRIGERATION_FAILURE`,
    `DOOR_OPENING`, `SENSOR_FAULT`, `UNKNOWN`;
  - `severity`: `LOW`, `MEDIUM` or `HIGH`;
  - `confidence`, between 0 and 1;
  - a `summary` in English, Spanish and Portuguese, the three languages the API already answers in;
  - a `recommendedAction`.
  
  An answer that does not parse, or that parses outside those sets, is discarded whole. It is never
  repaired.
- **A deterministic fallback, always.**
  - Rule-based triage takes over whenever the model times out, refuses, answers outside the schema,
    has no key configured, or has no context to read, because the API did not give it:
    - the severity comes from the peak and the duration against the frozen thresholds;
    - the cause is `UNKNOWN`;
    - the summary comes from a template.
  - Every alert records which one produced it: `source` is `LLM` or `RULES`.
  - **An alert always goes out.**
- **One model call per excursion, at the close.**
  - `ExcursionOpened` gets an immediate rule-based alert.
  - `ExcursionClosed`, when the duration and the whole window are known, is what goes to the model.
- **The model sees measurements, not people.** Temperatures, timestamps, the frozen thresholds and
  the *participation* of whoever held custody (`SHIPPER`, `CARRIER`, `CONSIGNEE`). No names, no
  emails, no documents, no identifiers of any sort.
- **The model is a port.**
  - Triage talks to it through Spring AI's chat model, behind a `TriageAdvisor` port, so the
    provider is an adapter.
  - The model is a property, `claude-opus-5-5` by default.
  - The prompt is a versioned file in the repository.
  - Every alert records the model, the prompt version, the token counts and the latency.
- **CI never calls a model.**
  - Tests stub the chat model.
  - Quality is measured apart from the build: a golden set of synthetic excursions, one or two per
    cause, run by hand with `evalTriage`. The confusion matrix is published in the triage README,
    failures included.

## Consequences

- **An alert can be wrong, and it says so.** The API names the field `probableCause` and serves the
  confidence next to it. Nothing reads it as a finding.
- **Changing the model or the prompt is a reviewed change**, and the evaluation runs again before
  it merges. A better score is the argument; a newer model is not.
- **The walkthrough and CI work without a key.** They exercise the fallback, so they assert the shape
  of the alert, never its wording.
- **Three summaries cost three times the output tokens** of one. Accepted: a summary is a few
  sentences, and translating at read time would put a model call on the API's request path, which is
  the thing ADR-010 exists to prevent.
- **The rule-based path is not a stub.** It is the answer half the time in CI and every time the
  model is down, so it is tested as seriously as the verdict.

## Alternatives considered and rejected

- **Letting the model narrate the certificate's findings.** The hash would cover text nobody can
  reproduce, and "defensible" would come to mean "plausible".
- **Free-text answers parsed with patterns.** That parser breaks on the first rephrasing, and the
  silent failure is a wrong cause shown with full confidence.
- **Rules only, no model.** They are cheaper, deterministic and already written: they are the
  fallback. What they cannot do is explain an excursion in a sentence the receiver reads in their own
  language, and that sentence is the reason a carrier opens the alert.
- **Fine-tuning a model.** There is no labelled history of excursion causes to tune it on, and the
  golden set is for measuring, not training.
- **Retrieval over past incidents.** There is nothing to retrieve yet. It is reconsidered when there
  are confirmed causes to learn from.
