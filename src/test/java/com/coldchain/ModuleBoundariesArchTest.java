package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleBoundariesArchTest {

    private static final String ROOT = "com.coldchain";

    private static final List<String> MODULES =
            List.of("identity", "catalog", "shipment", "telemetry", "compliance");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void moduleInternalsAreOnlyAccessedWithinTheirModule() {
        assertThat(inhabitedModules())
                .describedAs("a boundary nobody lives behind proves nothing")
                .isNotEmpty();

        inhabitedModules().forEach(module -> ArchRuleDefinition.noClasses()
                .that().resideOutsideOfPackage(ROOT + ".modules." + module + "..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".modules." + module + ".internal..")
                .because("the inside of a module is its own business, and delivery is outside it too")
                .check(production));
    }

    private List<String> inhabitedModules() {
        return MODULES.stream()
                .filter(module -> !production
                        .that(com.tngtech.archunit.base.DescribedPredicate.describe("internal of " + module,
                                javaClass -> javaClass.getPackageName()
                                        .startsWith(ROOT + ".modules." + module + ".internal")))
                        .isEmpty())
                .toList();
    }
}
