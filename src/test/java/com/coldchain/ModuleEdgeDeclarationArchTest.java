package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ModuleEdgeDeclarationArchTest {

    private static final String ROOT = "com.coldchain";

    private static final Path MODULES = SourceTree.MAIN.resolve(Path.of("com", "coldchain", "modules"));

    private static final List<String> DECLARED_MODULES =
            List.of("identity", "catalog", "shipment", "telemetry", "compliance");

    private static final Pattern ALLOWED_DEPENDENCIES =
            Pattern.compile("allowedDependencies[ ]*=[ ]*[{]([^}]*)[}]");

    private static final Pattern QUOTED = Pattern.compile("\"([^\"]+)\"");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT + ".modules");

    @Test
    void everyRealEdgeIsADeclaredEdge() {
        Map<String, Set<String>> declared = DECLARED_MODULES.stream()
                .collect(Collectors.toMap(module -> module, this::declaredEdgesOf));
        Map<String, Set<String>> real = DECLARED_MODULES.stream()
                .collect(Collectors.toMap(module -> module, this::realEdgesOf));

        assertThat(declared)
                .describedAs("every module declares its identity and its allowed dependencies")
                .containsOnlyKeys(DECLARED_MODULES.toArray(String[]::new));

        assertThat(real).allSatisfy((module, edges) -> assertThat(edges)
                .describedAs("module %s reaches modules it never declared", module)
                .isSubsetOf(declared.get(module)));

        assertThat(declared).allSatisfy((module, edges) -> assertThat(edges)
                .describedAs("module %s declares dependencies it does not use, "
                        + "which is a permission nobody asked for", module)
                .isSubsetOf(real.get(module)));
    }

    private Set<String> declaredEdgesOf(String module) {
        String source = SourceTree.read(MODULES.resolve(module).resolve("package-info.java"));
        assertThat(source)
                .describedAs("module %s declares no @ApplicationModule", module)
                .contains("@ApplicationModule")
                .contains("id = \"" + module + "\"");
        Matcher block = ALLOWED_DEPENDENCIES.matcher(source);
        assertThat(block.find())
                .describedAs("module %s declares no allowedDependencies", module)
                .isTrue();
        Set<String> edges = new TreeSet<>();
        Matcher quoted = QUOTED.matcher(block.group(1));
        while (quoted.find()) {
            edges.add(quoted.group(1));
        }
        return edges;
    }

    private Set<String> realEdgesOf(String module) {
        String ownPackage = ROOT + ".modules." + module;
        return production.stream()
                .filter(javaClass -> javaClass.getPackageName().startsWith(ownPackage))
                .flatMap(javaClass -> javaClass.getDirectDependenciesFromSelf().stream())
                .map(dependency -> dependency.getTargetClass())
                .map(JavaClass::getPackageName)
                .filter(name -> name.startsWith(ROOT + ".modules."))
                .map(name -> name.substring((ROOT + ".modules.").length()))
                .map(name -> name.contains(".") ? name.substring(0, name.indexOf('.')) : name)
                .filter(other -> !other.equals(module))
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
