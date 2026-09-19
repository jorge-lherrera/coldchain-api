package com.coldchain.bootstrap;

import com.coldchain.modules.catalog.CatalogModuleConfig;
import com.coldchain.modules.compliance.ComplianceModuleConfig;
import com.coldchain.modules.identity.IdentityModuleConfig;
import com.coldchain.modules.shipment.ShipmentModuleConfig;
import com.coldchain.modules.telemetry.TelemetryModuleConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The composition root. The application scans {@code bootstrap}, {@code shared} and
 * {@code delivery} and nothing else, so a module that is not on this list is not in the
 * application at all — which makes turning one off a deletion from here rather than a hunt
 * through the classpath. The order is the dependency order: nothing above depends on
 * anything below it.
 */
@Configuration
@Import({
        IdentityModuleConfig.class,
        CatalogModuleConfig.class,
        TelemetryModuleConfig.class,
        ShipmentModuleConfig.class,
        ComplianceModuleConfig.class
})
public class BootstrapConfiguration {
}
