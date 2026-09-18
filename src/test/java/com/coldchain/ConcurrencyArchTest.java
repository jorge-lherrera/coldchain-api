package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Version;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConcurrencyArchTest {

    private static final String ROOT = "com.coldchain";

    private static final String MODULES = ROOT + ".modules.";

    private static final String DOMAIN_MODEL = ".internal.domain.model";

    private static final String ENTITY_SUFFIX = "JpaEntity";

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void everyConcurrentlyWritableEntityIsVersioned() {
        List<JavaClass> models = production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .filter(type -> type.getPackageName().endsWith(DOMAIN_MODEL))
                .filter(type -> !type.isEnum() && !type.isInterface() && !type.isRecord())
                .toList();

        assertThat(models).isNotEmpty();
        assertThat(models.stream().filter(ConcurrencyArchTest::changesState).toList())
                .describedAs("no aggregate changes state, so this rule is watching nothing")
                .isNotEmpty();
        assertThat(models).allSatisfy(model -> entityOf(model).ifPresent(entity ->
                assertThat(carriesAVersion(entity))
                        .describedAs("%s is written more than once and %s does not carry a version, "
                                + "so the last write wins in silence and the first is lost with no "
                                + "trace", model.getSimpleName(), entity.getSimpleName())
                        .isEqualTo(changesState(model))));
    }

    private Optional<JavaClass> entityOf(JavaClass model) {
        return production.stream()
                .filter(type -> type.getSimpleName().equals(model.getSimpleName() + ENTITY_SUFFIX))
                .findFirst();
    }

    private static boolean changesState(JavaClass model) {
        return model.getMethods().stream()
                .filter(method -> !method.getModifiers().contains(JavaModifier.STATIC))
                .anyMatch(method -> method.getRawReturnType().equals(model));
    }

    private static boolean carriesAVersion(JavaClass entity) {
        return entity.getAllFields().stream().anyMatch(field -> field.isAnnotatedWith(Version.class));
    }
}
