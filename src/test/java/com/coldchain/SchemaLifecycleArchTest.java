package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SchemaLifecycleArchTest {

    private static final String ROOT = "com.coldchain";

    private static final List<Path> RESOURCE_FOLDERS = List.of(
            Path.of("src", "main", "resources"),
            Path.of("src", "test", "resources"),
            Path.of("src", "integrationTest", "resources"));

    private static final List<String> SETTINGS_THAT_ARE_NOT_NONE =
            List.of("ddl-auto: create", "ddl-auto: create-drop", "ddl-auto: update", "ddl-auto: validate");

    private static final List<String> STARTUP_WRITERS = List.of(
            "org.springframework.boot.CommandLineRunner",
            "org.springframework.boot.ApplicationRunner",
            "jakarta.annotation.PostConstruct");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void hibernateNeverWritesTheSchemaInAnyProfile() {
        List<Path> configurations = configurations();

        assertThat(configurations).isNotEmpty();
        assertThat(configurations).allSatisfy(file -> assertThat(SourceTree.read(file))
                .describedAs("%s lets Hibernate write the schema, so what runs in production is "
                        + "whatever the last entity happened to say", file)
                .doesNotContain(SETTINGS_THAT_ARE_NOT_NONE.toArray(String[]::new)));
        assertThat(configurations.stream()
                .map(SourceTree::read)
                .filter(text -> text.contains("ddl-auto"))
                .toList())
                .describedAs("no profile says what Hibernate may do to the schema")
                .isNotEmpty()
                .allSatisfy(text -> assertThat(text).contains("ddl-auto: none"));
    }

    @Test
    void everyMigrationIsNumberedAndUnique() {
        List<Migrations.Script> scripts = Migrations.scripts();

        assertThat(scripts).isNotEmpty();
        assertThat(scripts.stream().map(Migrations.Script::version).toList())
                .describedAs("the migrations do not run from one upwards without repeating or "
                        + "skipping a number")
                .containsExactlyElementsOf(Stream.iterate(1, version -> version + 1)
                        .limit(scripts.size())
                        .toList());
        assertThat(scripts.stream().map(Migrations.Script::name).toList())
                .describedAs("two migrations answer to the same name")
                .doesNotHaveDuplicates();
    }

    @Test
    void theDialectIsPinnedExplicitly() {
        assertThat(SourceTree.read(Path.of("src", "main", "resources", "application.yml")))
                .describedAs("without a pinned dialect the schema depends on the machine that "
                        + "produced it")
                .contains("dialect: org.hibernate.dialect.OracleDialect");
    }

    @Test
    void nothingSeedsRowsAtStartup() {
        ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat(DescribedPredicate.describe(
                        "are the hooks that would write rows before anybody asked",
                        type -> STARTUP_WRITERS.contains(type.getFullName())))
                .because("reference data ships in the migration that creates its table, where it "
                        + "is versioned like everything else")
                .check(production);
    }

    private static List<Path> configurations() {
        return RESOURCE_FOLDERS.stream()
                .filter(Files::isDirectory)
                .flatMap(SchemaLifecycleArchTest::listYaml)
                .toList();
    }

    private static Stream<Path> listYaml(Path folder) {
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".yml")).toList().stream();
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not list " + folder, cause);
        }
    }
}
