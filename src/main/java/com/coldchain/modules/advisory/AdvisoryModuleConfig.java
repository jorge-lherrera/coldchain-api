package com.coldchain.modules.advisory;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * The advice about each excursion, kept where the verdict cannot reach it. The module is in the
 * application because the composition root imports this class, and for no other reason.
 */
@Configuration
@ComponentScan(basePackageClasses = AdvisoryModuleConfig.class,
        excludeFilters = @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
public class AdvisoryModuleConfig {
}
