package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureRulesArchTest {

    private static final String ROOT = "com.coldchain";

    private static final Path MODULES = SourceTree.MAIN.resolve(Path.of("com", "coldchain", "modules"));

    private static final Path DELIVERY = SourceTree.MAIN.resolve(Path.of("com", "coldchain", "delivery"));

    private static final List<String> FORBIDDEN_API_SUFFIXES =
            List.of("Dto", "Request", "Response", "Mapper", "Service", "Query", "Ref");

    private static final List<String> FRAMEWORK_PACKAGES = List.of(
            "org.springframework..", "jakarta.persistence..", "jakarta.validation..",
            "org.hibernate..", "tools.jackson..", "com.fasterxml.jackson..");

    private static final List<String> INJECTION_ANNOTATIONS = List.of(
            "org.springframework.beans.factory.annotation.Autowired",
            "jakarta.inject.Inject",
            "jakarta.annotation.Resource");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void modulesAreFreeOfCycles() {
        SlicesRuleDefinition.slices()
                .matching(ROOT + ".modules.(*)..")
                .should().beFreeOfCycles()
                .because("two modules that need each other are one module nobody drew")
                .check(production);
    }

    @Test
    void apiPackagesNeverDependOnInternals() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".modules.*.api..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".modules..internal..")
                .because("the contract does not drag the implementation behind it")
                .check(production);
    }

    @Test
    void apiInterfacesAreNamedApi() {
        inhabitedModules().forEach(module -> {
            Path door = MODULES.resolve(module).resolve("api")
                    .resolve(capitalise(module) + "Api.java");
            assertThat(door)
                    .describedAs("module %s must publish exactly one door, %sApi", module, capitalise(module))
                    .exists();
            assertThat(SourceTree.read(door))
                    .describedAs("the door is an interface, never a class")
                    .contains("public interface " + capitalise(module) + "Api");
        });

        List<String> misplaced = SourceTree.javaFiles(MODULES).stream()
                .filter(source -> source.getFileName().toString().endsWith("Api.java"))
                .filter(source -> !SourceTree.packageOf(source).endsWith(".api"))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(misplaced).describedAs("a door outside api/ is a door around the contract").isEmpty();
    }

    @Test
    void useCaseClassesAreNamedUseCase() {
        List<Path> useCases = SourceTree.javaFiles(MODULES).stream()
                .filter(source -> source.toString().contains("usecase"))
                .toList();

        assertThat(useCases).describedAs("there are use cases to check").isNotEmpty();
        assertThat(useCases).allSatisfy(source -> {
            assertThat(source.getFileName().toString())
                    .describedAs("%s lives under usecase/ and must say so in its name", source)
                    .endsWith("UseCase.java");
            assertThat(SourceTree.read(source).lines().filter(line -> line.contains(" execute(")).count())
                    .describedAs("%s must do one thing through one execute", source)
                    .isEqualTo(1);
        });
    }

    @Test
    void repositoryPortsAreInterfaces() {
        List<Path> ports = SourceTree.javaFiles(MODULES).stream()
                .filter(source -> source.toString().contains("domain") && source.toString().contains("repository"))
                .toList();

        assertThat(ports).describedAs("there are ports to check").isNotEmpty();
        assertThat(ports).allSatisfy(source -> {
            assertThat(source.getFileName().toString()).endsWith("Repository.java");
            assertThat(SourceTree.read(source))
                    .describedAs("%s is a port, so it is an interface the domain owns", source)
                    .contains("public interface ");
        });
    }

    @Test
    void apiPackagesHaveNoForbiddenSuffixes() {
        List<String> named = SourceTree.javaFiles(MODULES).stream()
                .filter(source -> SourceTree.packageOf(source).contains(".api"))
                .map(source -> source.getFileName().toString().replace(".java", ""))
                .filter(name -> FORBIDDEN_API_SUFFIXES.stream().anyMatch(name::endsWith))
                .toList();

        assertThat(named)
                .describedAs("api/ speaks the language of the contract, not of HTTP or of an implementation")
                .isEmpty();
    }

    @Test
    void noFieldInjection() {
        INJECTION_ANNOTATIONS.forEach(annotation -> ArchRuleDefinition.noFields()
                .should().beAnnotatedWith(annotation)
                .because("a dependency that is not in the constructor is one the compiler cannot demand")
                .check(production));
    }

    @Test
    void noSetterInjection() {
        INJECTION_ANNOTATIONS.forEach(annotation -> ArchRuleDefinition.noMethods()
                .should().beAnnotatedWith(annotation)
                .because("an object that can be reconfigured after construction has no invariants")
                .check(production));
    }

    @Test
    void domainModelsAreFrameworkFree() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".modules..internal.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(FRAMEWORK_PACKAGES.toArray(String[]::new))
                .because("the rules must be testable without booting anything")
                .check(production);
    }

    @Test
    void persistenceDeclaresNoTransactions() {
        List<String> annotated = SourceTree.javaFiles(MODULES).stream()
                .filter(source -> source.toString().contains("persistence"))
                .filter(source -> SourceTree.read(source).contains("Transactional"))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(annotated)
                .describedAs("the transaction is the use case's decision, and an adapter cannot take it")
                .isEmpty();
    }

    @Test
    void pageableEndpointsDeclareSortableFields() {
        List<Path> paginating = SourceTree.javaFiles(DELIVERY).stream()
                .filter(source -> SourceTree.read(source).contains("Pageable"))
                .toList();

        assertThat(paginating).describedAs("there is a paginated endpoint to check").isNotEmpty();
        assertThat(paginating).allSatisfy(source -> assertThat(SourceTree.read(source))
                .describedAs("%s takes a sort it never bounded: an open sort orders by any column",
                        source.getFileName())
                .contains("SortCatalog"));
    }

    @Test
    void deliveryNeverDependsOnUseCases() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".delivery..")
                .should().dependOnClassesThat().resideInAPackage("..usecase..")
                .because("a controller asks the module's door, not the room behind it")
                .check(production);
    }

    @Test
    void deliveryNeverDependsOnModuleDomain() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".delivery..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".modules..internal..")
                .because("a domain model that reaches a controller becomes part of the wire format")
                .check(production);
    }

    private List<String> inhabitedModules() {
        return SourceTree.javaFiles(MODULES).stream()
                .map(source -> SourceTree.packageOf(source).substring((ROOT + ".modules.").length()))
                .map(name -> name.contains(".") ? name.substring(0, name.indexOf('.')) : name)
                .filter(module -> !SourceTree.javaFiles(MODULES.resolve(module)).stream()
                        .allMatch(source -> source.getFileName().toString().equals("package-info.java")))
                .distinct()
                .toList();
    }

    private static String capitalise(String module) {
        return Character.toUpperCase(module.charAt(0)) + module.substring(1);
    }
}
