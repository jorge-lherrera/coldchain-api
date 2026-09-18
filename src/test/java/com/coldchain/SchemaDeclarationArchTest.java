package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchemaDeclarationArchTest {

    private static final String ROOT = "com.coldchain";

    private static final String TENANT_COLUMN = "ORGANIZATION_ID";

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void tenantColumnIndexIsDeclaredOnTheEntity() {
        List<JavaClass> readByTenant = production.stream()
                .filter(type -> type.isAnnotatedWith(Entity.class))
                .filter(SchemaDeclarationArchTest::mapsTheTenantColumn)
                .filter(this::isReadByTenant)
                .toList();

        assertThat(readByTenant).isNotEmpty();
        assertThat(readByTenant).allSatisfy(entity -> {
            assertThat(indexedColumnsOf(entity))
                    .describedAs("%s is read by tenant and says so only in the DDL, where the code "
                            + "cannot see it", entity.getFullName())
                    .contains(TENANT_COLUMN);
            assertThat(Migrations.indexes().stream()
                    .filter(index -> index.table().equals(tableOf(entity)))
                    .anyMatch(index -> index.name().endsWith(TENANT_COLUMN)))
                    .describedAs("%s is read by tenant without an index to do it with", tableOf(entity))
                    .isTrue();
        });
    }

    private boolean isReadByTenant(JavaClass entity) {
        String repository = entity.getSimpleName().replace("JpaEntity", "JpaRepository");
        return production.stream()
                .filter(type -> type.getSimpleName().equals(repository))
                .flatMap(type -> type.getMethods().stream())
                .anyMatch(method -> method.getName().contains("OrganizationId"));
    }

    private static String tableOf(JavaClass entity) {
        return entity.getAnnotationOfType(Table.class).name();
    }

    private static boolean mapsTheTenantColumn(JavaClass entity) {
        return entity.getAllFields().stream()
                .filter(field -> field.isAnnotatedWith(Column.class))
                .anyMatch(field -> field.getAnnotationOfType(Column.class).name().equals(TENANT_COLUMN));
    }

    private static List<String> indexedColumnsOf(JavaClass entity) {
        if (!entity.isAnnotatedWith(Table.class)) {
            return List.of();
        }
        return Arrays.stream(entity.getAnnotationOfType(Table.class).indexes())
                .map(Index::columnList)
                .flatMap(columns -> Arrays.stream(columns.split(",")))
                .map(String::trim)
                .toList();
    }
}
