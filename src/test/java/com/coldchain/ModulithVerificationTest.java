package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithVerificationTest {

    private static final List<String> DECLARED_MODULES =
            List.of("identity", "catalog", "shipment", "telemetry", "compliance");

    private final ApplicationModules modules = ApplicationModules.of(ColdChainApplication.class);

    @Test
    void theModuleModelBuilds() {
        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .containsExactlyInAnyOrderElementsOf(DECLARED_MODULES);
    }

    @Test
    void everyDeclaredDependencyIsARealOneAndEveryRealOneIsDeclared() {
        modules.verify();
    }
}
