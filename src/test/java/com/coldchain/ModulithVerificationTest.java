package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;


class ModulithVerificationTest {

    private static final List<String> DECLARED_MODULES =
            List.of("identity", "catalog", "shipment", "telemetry", "compliance");

    private static final String COMPOSITION_ROOT = "bootstrap";

    private final ApplicationModules modules = ApplicationModules.of(ColdChainApplication.class);

    @Test
    void theModuleModelBuilds() {
        List<String> expected = new ArrayList<>(DECLARED_MODULES);
        expected.add(COMPOSITION_ROOT);

        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .describedAs("the model is the five modules and the root that assembles them")
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void everyDeclaredDependencyIsARealOneAndEveryRealOneIsDeclared() {
        modules.verify();
    }
}
