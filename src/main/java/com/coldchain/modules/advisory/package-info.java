/**
 * The advice the triage service sends about each excursion: a probable cause, how serious it looks and
 * what to do next. Advice, not evidence: nothing depends on this module, and the verdict least of all
 * (ADR-012).
 */
@ApplicationModule(id = "advisory", allowedDependencies = {"shipment::api"})
package com.coldchain.modules.advisory;

import org.springframework.modulith.ApplicationModule;
