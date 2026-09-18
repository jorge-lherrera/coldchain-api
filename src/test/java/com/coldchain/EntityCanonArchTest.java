package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.shared.persistence.AuditableEntity;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class EntityCanonArchTest {

    private static final String ROOT = "com.coldchain";

    private static final String MODULES = ROOT + ".modules.";

    private static final String DOMAIN_MODEL = ".internal.domain.model";

    private static final Pattern FOREIGN_KEY = Pattern.compile(
            "CONSTRAINT [A-Z0-9_]+ FOREIGN KEY [(]([A-Z0-9_]+)[)] REFERENCES ([A-Z0-9_]+)");

    private static final List<String> RELATION_ANNOTATIONS = List.of(
            "jakarta.persistence.ManyToOne",
            "jakarta.persistence.OneToMany",
            "jakarta.persistence.OneToOne",
            "jakarta.persistence.ManyToMany",
            "jakarta.persistence.ElementCollection",
            "jakarta.persistence.Embedded");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void everyEntityIsAuditable() {
        List<JavaClass> entities = entities();

        assertThat(entities).isNotEmpty();
        assertThat(entities).allSatisfy(entity -> assertThat(entity.isAssignableTo(AuditableEntity.class))
                .describedAs("%s maps a table without carrying the four columns that say who wrote "
                        + "the row and when", entity.getFullName())
                .isTrue());
    }

    @Test
    void everyRestrictedColumnIsMapped() {
        List<JavaClass> entities = entities();

        assertThat(entities).isNotEmpty();
        assertThat(entities).allSatisfy(entity -> {
            List<String> mapped = mappedColumnsOf(entity);
            assertThat(indexedColumnsOf(entity))
                    .describedAs("%s indexes a column it does not map, and every SELECT would die "
                            + "of ORA-00904 without the compiler noticing", entity.getFullName())
                    .isSubsetOf(mapped);
        });
    }

    @Test
    void everyReferenceColumnIsNamedAfterItsTarget() {
        List<Migrations.Table> tables = Migrations.tables();

        assertThat(tables).isNotEmpty();
        assertThat(tables).allSatisfy(table -> {
            Matcher matcher = FOREIGN_KEY.matcher(table.body().replaceAll("\\s+", " "));
            while (matcher.find()) {
                String column = matcher.group(1);
                String target = matcher.group(2);
                if (column.endsWith("_BY")) {
                    continue;
                }
                assertThat(column)
                        .describedAs("%s.%s points at %s without saying so", table.name(), column, target)
                        .endsWith(target + "_ID");
            }
        });
    }

    @Test
    void noEntityHoldsAnotherModulesEntity() {
        ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat(DescribedPredicate.describe(
                        "are the annotations that turn an identifier into an object graph",
                        type -> RELATION_ANNOTATIONS.contains(type.getFullName())))
                .because("an identifier from another bounded context is a UUID column with no "
                        + "foreign key, never an object the mapper walks into")
                .check(production);
        assertThat(entities()).allSatisfy(entity -> assertThat(entity.getFields())
                .describedAs("%s holds another entity instead of its identifier", entity.getFullName())
                .noneMatch(field -> field.getRawType().isAnnotatedWith(Entity.class)));
    }

    @Test
    void frozenColumnsHaveNoSetter() {
        List<JavaClass> entities = entities();
        List<JavaClass> models = production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .filter(type -> type.getPackageName().endsWith(DOMAIN_MODEL))
                .filter(type -> !type.isEnum() && !type.isInterface())
                .toList();

        assertThat(entities).isNotEmpty();
        assertThat(models).isNotEmpty();
        assertThat(entities).allSatisfy(entity -> assertThat(entity.getMethods())
                .describedAs("%s lets a column be written twice", entity.getFullName())
                .noneMatch(method -> method.getName().startsWith("set")));
        assertThat(models).allSatisfy(model -> assertThat(model.getFields())
                .describedAs("%s can change under the caller's feet, so a value frozen at dispatch "
                        + "is only frozen by agreement", model.getFullName())
                .allMatch(field -> field.getModifiers().contains(JavaModifier.FINAL)));
    }

    private List<JavaClass> entities() {
        return production.stream()
                .filter(type -> type.isAnnotatedWith(Entity.class))
                .toList();
    }

    private static List<String> mappedColumnsOf(JavaClass entity) {
        return entity.getAllFields().stream()
                .filter(field -> field.isAnnotatedWith(Column.class))
                .map(field -> field.getAnnotationOfType(Column.class).name())
                .toList();
    }

    private static List<String> indexedColumnsOf(JavaClass entity) {
        if (!entity.isAnnotatedWith(Table.class)) {
            return List.of();
        }
        return Arrays.stream(entity.getAnnotationOfType(Table.class).indexes())
                .map(Index::columnList)
                .flatMap(columns -> Arrays.stream(columns.split(",")))
                .map(String::trim)
                .filter(column -> !column.isBlank())
                .toList();
    }
}
