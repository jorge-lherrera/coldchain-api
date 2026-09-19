package com.coldchain.modules.telemetry;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * What the sensors measured, and where it left the band. The module is in the application because the composition root
 * imports this class, and for no other reason.
 */
@Configuration
@ComponentScan(basePackageClasses = TelemetryModuleConfig.class,
        excludeFilters = @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
public class TelemetryModuleConfig {
}
