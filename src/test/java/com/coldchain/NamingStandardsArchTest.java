package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class NamingStandardsArchTest {

    private static final String ROOT = "com.coldchain";

    private static final String ENTITY_SUFFIX = "JpaEntity";

    private static final Pattern UPPER_SNAKE_CASE = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");

    private static final List<String> VERB_PREFIXES = List.of("is", "has", "can", "should", "was", "does");

    private static final List<String> LEGACY_TIME_TYPES = List.of(
            "java.util.Date",
            "java.util.Calendar",
            "java.sql.Date",
            "java.sql.Time",
            "java.sql.Timestamp",
            "java.time.LocalDateTime");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void booleanFieldsHaveNoVerbPrefix() {
        List<String> fields = production.stream()
                .flatMap(type -> type.getFields().stream())
                .map(JavaField::getFullName)
                .toList();
        List<String> accessors = production.stream()
                .flatMap(type -> type.getMethods().stream())
                .filter(method -> method.getRawParameterTypes().isEmpty())
                .map(JavaMethod::getFullName)
                .toList();

        assertThat(fields).isNotEmpty();
        assertThat(accessors).isNotEmpty();
        assertThat(fields).allSatisfy(NamingStandardsArchTest::carriesNoVerbPrefix);
        assertThat(accessors).allSatisfy(NamingStandardsArchTest::carriesNoVerbPrefix);
        assertThat(persistenceNames()).allSatisfy(name -> assertThat(startsWithAVerb(camelCaseOf(name)))
                .describedAs("column %s names a fact with a verb", name)
                .isFalse());
    }

    @Test
    void timestampsAvoidLegacyDateTypes() {
        ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat(DescribedPredicate.describe(
                        "are the date and time types Java replaced",
                        type -> LEGACY_TIME_TYPES.contains(type.getFullName())))
                .because("an instant is an Instant: the legacy types carry a zone nobody declared")
                .check(production);
    }

    @Test
    void jpaEntitiesUseJpaEntitySuffix() {
        List<JavaClass> entities = production.stream()
                .filter(type -> type.isAnnotatedWith(Entity.class))
                .toList();

        assertThat(entities).isNotEmpty();
        assertThat(entities).allSatisfy(entity -> assertThat(entity.getSimpleName())
                .describedAs("%s maps a table and does not say so in its name", entity.getFullName())
                .endsWith(ENTITY_SUFFIX));
    }

    @Test
    void persistenceNamesAreUpperSnakeCase() {
        List<String> names = persistenceNames();

        assertThat(names).isNotEmpty();
        assertThat(names).allSatisfy(name -> assertThat(UPPER_SNAKE_CASE.matcher(name).matches())
                .describedAs("persistence name %s is not UPPER_SNAKE_CASE", name)
                .isTrue());
    }

    private List<String> persistenceNames() {
        List<String> names = new ArrayList<>();
        production.stream()
                .filter(type -> type.isAnnotatedWith(Table.class))
                .forEach(type -> names.add(type.getAnnotationOfType(Table.class).name()));
        production.stream()
                .flatMap(type -> type.getFields().stream())
                .forEach(field -> {
                    if (field.isAnnotatedWith(Column.class)) {
                        names.add(field.getAnnotationOfType(Column.class).name());
                    }
                    if (field.isAnnotatedWith(JoinColumn.class)) {
                        names.add(field.getAnnotationOfType(JoinColumn.class).name());
                    }
                });
        return names.stream().filter(name -> !name.isBlank()).toList();
    }

    private static void carriesNoVerbPrefix(String fullName) {
        String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1).replace("()", "");
        String withoutAccessorPrefix = simpleName.startsWith("get") && simpleName.length() > 3
                ? Character.toLowerCase(simpleName.charAt(3)) + simpleName.substring(4)
                : simpleName;
        assertThat(startsWithAVerb(withoutAccessorPrefix))
                .describedAs("%s names a fact with a verb: the JSON reads better without it", fullName)
                .isFalse();
    }

    private static boolean startsWithAVerb(String name) {
        return VERB_PREFIXES.stream().anyMatch(prefix -> name.length() > prefix.length()
                && name.startsWith(prefix)
                && Character.isUpperCase(name.charAt(prefix.length())));
    }

    private static String camelCaseOf(String upperSnakeCase) {
        String[] words = upperSnakeCase.toLowerCase().split("_");
        StringBuilder camelCase = new StringBuilder(words[0]);
        for (int index = 1; index < words.length; index++) {
            camelCase.append(Character.toUpperCase(words[index].charAt(0))).append(words[index].substring(1));
        }
        return camelCase.toString();
    }
}
