package com.coldchain.modules.catalog;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * What is shipped and under what conditions. The module is in the application because the composition root
 * imports this class, and for no other reason.
 */
@Configuration
@ComponentScan(basePackageClasses = CatalogModuleConfig.class,
        excludeFilters = @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
public class CatalogModuleConfig {
}
