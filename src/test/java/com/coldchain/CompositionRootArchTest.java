package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CompositionRootArchTest {

    private static final List<String> DECLARED_MODULES =
            List.of("identity", "catalog", "shipment", "telemetry", "compliance");

    private static final List<String> SCANNED_PACKAGES =
            List.of("com.coldchain.bootstrap", "com.coldchain.shared", "com.coldchain.delivery");

    private final String application = SourceTree.read(
            Path.of("src", "main", "java", "com", "coldchain", "ColdChainApplication.java"));

    private final String compositionRoot = SourceTree.read(
            Path.of("src", "main", "java", "com", "coldchain", "bootstrap",
                    "BootstrapConfiguration.java"));

    @Test
    void theApplicationScansTheRootTheSharedKernelAndDeliveryAndNothingElse() {
        assertThat(SCANNED_PACKAGES).allSatisfy(scanned -> assertThat(application)
                .describedAs("the application has to scan %s for the assembly to hold", scanned)
                .contains("\"" + scanned + "\""));
        assertThat(application)
                .describedAs("scanning modules/ directly puts a module in the application whether "
                        + "the composition root names it or not, which is the same as having no "
                        + "composition root")
                .doesNotContain("\"com.coldchain.modules");
        assertThat(application.replaceAll("\\s+", ""))
                .describedAs("a bare @SpringBootApplication scans everything below it")
                .contains("@SpringBootApplication(scanBasePackages=");
    }

    @Test
    void everyModuleEntersTheApplicationThroughTheCompositionRoot() {
        assertThat(DECLARED_MODULES).allSatisfy(module -> {
            String configuration = capitalised(module) + "ModuleConfig";
            Optional<Path> source = SourceTree.find(configuration, SourceTree.MAIN);

            assertThat(source)
                    .describedAs("module %s has no configuration, so it can only enter the "
                            + "application by being scanned from somewhere it does not control",
                            module)
                    .isPresent();
            assertThat(SourceTree.packageOf(source.orElseThrow()))
                    .isEqualTo("com.coldchain.modules." + module);
            assertThat(SourceTree.read(source.orElseThrow()))
                    .describedAs("%s must scan its own module and only its own module", configuration)
                    .contains("basePackageClasses = " + configuration + ".class");
            assertThat(compositionRoot)
                    .describedAs("module %s is not on the import list, so it is not in the "
                            + "application", module)
                    .contains(configuration + ".class");
        });
    }

    @Test
    void theCompositionRootImportsNothingItDoesNotDeclare() {
        List<String> imported = compositionRoot.lines()
                .map(String::strip)
                .filter(line -> line.endsWith("ModuleConfig.class,") || line.endsWith("ModuleConfig.class"))
                .map(line -> line.replace("ModuleConfig.class,", "").replace("ModuleConfig.class", ""))
                .map(String::toLowerCase)
                .toList();

        assertThat(imported)
                .describedAs("the list in the composition root is the list of modules, in the order "
                        + "they depend on each other")
                .containsExactly("identity", "catalog", "telemetry", "shipment", "compliance");
    }

    private static String capitalised(String module) {
        return Character.toUpperCase(module.charAt(0)) + module.substring(1);
    }
}
