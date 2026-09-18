package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PersistenceNamingCanonArchTest {

    private static final String ROOT = "com.coldchain";

    private static final List<String> WORDS_THAT_END_IN_S = List.of("STATUS", "ADDRESS");

    private static final List<String> INTEGRITY_KEYWORDS =
            List.of("PRIMARY KEY", "FOREIGN KEY", "REFERENCES", "CHECK", "UNIQUE");

    private static final List<Map.Entry<String, String>> PREFIX_OF_CLAUSE = List.of(
            Map.entry("PRIMARY KEY", "PK"),
            Map.entry("FOREIGN KEY", "FK"),
            Map.entry("CHECK", "CK"),
            Map.entry("UNIQUE", "UQ"));

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void tableNamesAreSingular() {
        List<String> mapped = production.stream()
                .filter(type -> type.isAnnotatedWith(Table.class))
                .map(type -> type.getAnnotationOfType(Table.class).name())
                .toList();
        List<String> created = Migrations.tables().stream().map(Migrations.Table::name).toList();

        assertThat(mapped).isNotEmpty();
        assertThat(created).isNotEmpty();
        assertThat(mapped).allSatisfy(PersistenceNamingCanonArchTest::readsAsOneRow);
        assertThat(created).allSatisfy(PersistenceNamingCanonArchTest::readsAsOneRow);
    }

    @Test
    void everyDeclaredConstraintCarriesACanonicalName() {
        List<Migrations.Table> tables = Migrations.tables();
        List<Migrations.Index> indexes = Migrations.indexes();

        assertThat(tables).isNotEmpty();
        assertThat(indexes).isNotEmpty();
        assertThat(tables).allSatisfy(table -> clausesOf(table.body()).stream()
                .filter(PersistenceNamingCanonArchTest::guardsIntegrity)
                .forEach(clause -> assertName(table, clause)));
        assertThat(indexes).allSatisfy(index -> assertThat(index.name())
                .describedAs("index %s on %s does not say what it indexes", index.name(), index.table())
                .startsWith((index.unique() ? "UX_" : "IX_") + index.table() + "_"));
    }

    private static void assertName(Migrations.Table table, String clause) {
        assertThat(clause)
                .describedAs("a constraint of %s is declared without a name, "
                        + "so the database invents one and a production failure is investigated "
                        + "instead of read", table.name())
                .startsWith("CONSTRAINT ");
        String name = clause.split("\\s+")[1];
        String expectedPrefix = PREFIX_OF_CLAUSE.stream()
                .filter(entry -> clause.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow();
        assertThat(name)
                .describedAs("constraint %s on %s does not follow <PREFIX>_<TABLE>_<WHAT>", name, table.name())
                .startsWith(expectedPrefix + "_" + table.name());
    }

    private static boolean guardsIntegrity(String clause) {
        return INTEGRITY_KEYWORDS.stream().anyMatch(clause::contains);
    }

    private static void readsAsOneRow(String name) {
        String lastWord = name.substring(name.lastIndexOf('_') + 1);
        assertThat(lastWord.endsWith("S") && !WORDS_THAT_END_IN_S.contains(lastWord))
                .describedAs("table %s is named in the plural: a row is one of the thing, not many", name)
                .isFalse();
    }

    private static List<String> clausesOf(String body) {
        List<String> clauses = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (char character : body.toCharArray()) {
            if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;
            }
            if (character == ',' && depth == 0) {
                clauses.add(normalise(current.toString()));
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        clauses.add(normalise(current.toString()));
        return clauses.stream().filter(clause -> !clause.isBlank()).toList();
    }

    private static String normalise(String clause) {
        return clause.replaceAll("\\s+", " ").trim();
    }
}
