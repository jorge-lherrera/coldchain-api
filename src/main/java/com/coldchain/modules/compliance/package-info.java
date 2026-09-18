/**
 * The verdict a regulator reads: what the series says about the promise the shipment made.
 */
@ApplicationModule(id = "compliance", allowedDependencies = {"shipment::api", "telemetry::api"})
package com.coldchain.modules.compliance;

import org.springframework.modulith.ApplicationModule;
