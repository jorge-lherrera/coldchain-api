package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.shared.application.UseCase;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

class ApiContractCanonArchTest {

    private static final String ROOT = "com.coldchain";

    private static final String MODULES = ROOT + ".modules.";

    private static final List<String> CONTRACT_SUFFIXES =
            List.of("Api", "Command", "Filter", "Result", "Policy");

    private static final List<String> BANNED_READ_SUFFIXES =
            List.of("Summary", "View", "Row", "Detail", "Info", "Data", "Dto", "Payload");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void apiPackagesCarryOnlyTheSevenAllowedSuffixes() {
        List<JavaClass> door = contractTypes(".api");
        List<JavaClass> dtos = contractTypes(".api.dto");
        List<JavaClass> events = contractTypes(".api.event");

        assertThat(door).isNotEmpty();
        assertThat(dtos).isNotEmpty();
        assertThat(events).isNotEmpty();
        assertThat(door).allSatisfy(type -> assertThat(type.isEnum() || type.getSimpleName().endsWith("Api"))
                .describedAs("%s is neither the module's door nor one of its enums", type.getFullName())
                .isTrue());
        assertThat(dtos).allSatisfy(dto -> assertThat(namesARole(dto) || isAValueOfTheContract(dto, dtos))
                .describedAs("%s is not a command, a filter, a result, or a value one of them carries",
                        dto.getFullName())
                .isTrue());
        assertThat(events).allSatisfy(event -> assertThat(isTheEventInterface(event)
                || implementsTheEventInterface(event))
                .describedAs("%s sits among the published facts without being one", event.getFullName())
                .isTrue());
        assertThat(production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .filter(type -> !type.getPackageName().endsWith(".api.event"))
                .filter(ApiContractCanonArchTest::implementsTheEventInterface)
                .toList())
                .describedAs("a published fact lives outside the folder the contract publishes")
                .isEmpty();
    }

    @Test
    void everyReadDtoConvergesOnResult() {
        List<JavaClass> contract = production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .filter(type -> type.getPackageName().contains(".api"))
                .toList();

        assertThat(contract).isNotEmpty();
        assertThat(contract).allSatisfy(type -> assertThat(BANNED_READ_SUFFIXES)
                .describedAs("%s gives a read DTO a second name: at the point of use nobody can "
                        + "tell which of the two may cross the boundary", type.getFullName())
                .noneSatisfy(banned -> assertThat(type.getSimpleName()).endsWith(banned)));
    }

    @Test
    void useCasesAreAnnotatedAsUseCasesNotAsServices() {
        List<JavaClass> useCases = production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .filter(type -> type.getSimpleName().endsWith("UseCase"))
                .toList();
        List<JavaClass> insideModules = production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .toList();

        assertThat(useCases).isNotEmpty();
        assertThat(useCases).allSatisfy(useCase -> assertThat(useCase.isAnnotatedWith(UseCase.class))
                .describedAs("%s is a use case and does not say so", useCase.getFullName())
                .isTrue());
        assertThat(insideModules).allSatisfy(type -> assertThat(type.isAnnotatedWith(Service.class))
                .describedAs("%s is annotated @Service, which says nothing about what it is",
                        type.getFullName())
                .isFalse());
    }

    private List<JavaClass> contractTypes(String folder) {
        return production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .filter(type -> type.getPackageName().endsWith(folder))
                .filter(type -> !type.getSimpleName().equals("package-info"))
                .toList();
    }

    private static boolean namesARole(JavaClass type) {
        return CONTRACT_SUFFIXES.stream().anyMatch(suffix -> type.getSimpleName().endsWith(suffix));
    }

    private static boolean isAValueOfTheContract(JavaClass value, List<JavaClass> dtos) {
        return dtos.stream()
                .filter(ApiContractCanonArchTest::namesARole)
                .anyMatch(dto -> dto.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dependency -> dependency.getTargetClass().equals(value)));
    }

    private static boolean isTheEventInterface(JavaClass type) {
        return type.isInterface() && type.getSimpleName().endsWith("Event");
    }

    private static boolean implementsTheEventInterface(JavaClass type) {
        return type.getAllRawInterfaces().stream()
                .anyMatch(ApiContractCanonArchTest::isTheEventInterface);
    }
}
