package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModuleShapeArchTest {

    private static final Path MODULES = SourceTree.MAIN.resolve(Path.of("com", "coldchain", "modules"));

    private static final List<String> DECLARED_MODULES =
            List.of("identity", "catalog", "shipment", "telemetry", "compliance");

    private static final Pattern ONE_LOWERCASE_WORD = Pattern.compile("[a-z]+");

    private static final List<String> MODULE_FOLDERS = List.of("api", "internal");

    private static final List<String> INTERNAL_FOLDERS =
            List.of("domain", "application", "infrastructure", "exception");

    private static final Map<String, List<String>> FIXED_LAYOUT = Map.of(
            "internal/domain", List.of("model", "repository", "service"),
            "internal/application", List.of("usecase", "mapper"),
            "internal/application/usecase", List.of("command", "query"),
            "internal/infrastructure", List.of("persistence"),
            "internal/infrastructure/persistence", List.of("entity", "jpa", "adapter", "mapper"));

    @Test
    void everyModuleIsNamedInOneLowercaseWord() {
        List<String> found = folderNames(MODULES);

        assertThat(found).isNotEmpty().containsExactlyInAnyOrderElementsOf(DECLARED_MODULES);
        assertThat(found).allSatisfy(name -> assertThat(ONE_LOWERCASE_WORD.matcher(name).matches())
                .describedAs("module %s is not one lowercase word", name)
                .isTrue());
    }

    @Test
    void everyModuleHasExactlyApiAndInternal() {
        assertThat(DECLARED_MODULES).allSatisfy(module -> assertThat(folderNames(MODULES.resolve(module)))
                .describedAs("module %s", module)
                .containsExactlyInAnyOrderElementsOf(MODULE_FOLDERS));
    }

    @Test
    void everyInternalHoldsOnlyItsFourFolders() {
        assertThat(DECLARED_MODULES)
                .allSatisfy(module -> assertThat(folderNames(MODULES.resolve(module).resolve("internal")))
                        .describedAs("module %s", module)
                        .isSubsetOf(INTERNAL_FOLDERS));
    }

    @Test
    void everyLayerKeepsItsFixedLayout() {
        assertThat(DECLARED_MODULES).allSatisfy(module -> FIXED_LAYOUT
                .forEach((layer, allowed) -> assertThat(folderNames(MODULES.resolve(module).resolve(layer)))
                        .describedAs("%s of module %s", layer, module)
                        .isSubsetOf(allowed)));
    }

    @Test
    void sharedHoldsNoBusiness() {
        JavaClasses shared = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.coldchain.shared");

        ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat().resideInAPackage("com.coldchain.modules..")
                .because("shared holds value types and cross-cutting contracts, never a module's business")
                .check(shared);
    }

    private static List<String> folderNames(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not list " + directory, cause);
        }
    }
}
