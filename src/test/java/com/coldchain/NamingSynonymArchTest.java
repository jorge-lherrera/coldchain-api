package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class NamingSynonymArchTest {

    private static final String ROOT = "com.coldchain";

    private static final Pattern COLUMN_LINE = Pattern.compile("^ {4}([A-Z][A-Z0-9_]*) {2,}[A-Z]");

    private static final Map<String, String> BANNED_ANYWHERE = Map.ofEntries(
            Map.entry("ORG", "ORGANIZATION"),
            Map.entry("USR", "USER"),
            Map.entry("QTY", "QUANTITY"),
            Map.entry("AMT", "AMOUNT"),
            Map.entry("NUM", "NUMBER"),
            Map.entry("NO", "NUMBER"),
            Map.entry("DESC", "DESCRIPTION"),
            Map.entry("IDX", "INDEX"),
            Map.entry("MSG", "MESSAGE"),
            Map.entry("ADDR", "ADDRESS"),
            Map.entry("CNT", "COUNT"),
            Map.entry("VAL", "VALUE"),
            Map.entry("TMP", "TEMPORARY"),
            Map.entry("CFG", "CONFIGURATION"),
            Map.entry("PARAM", "PARAMETER"),
            Map.entry("ATTR", "ATTRIBUTE"));

    private static final Map<String, String> BANNED_AS_UNIT = Map.of(
            "MIN", "MINUTES",
            "SEC", "SECONDS",
            "MS", "MILLISECONDS",
            "HR", "HOURS",
            "PCT", "PERCENT");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void fieldsAndColumnsDoNotUseBannedSynonyms() {
        List<String> columns = Migrations.scripts().stream()
                .flatMap(script -> script.text().lines())
                .map(COLUMN_LINE::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .toList();
        List<String> fields = production.stream()
                .flatMap(type -> type.getFields().stream())
                .filter(field -> !isEnumConstant(field))
                .map(JavaField::getFullName)
                .toList();

        assertThat(columns).isNotEmpty();
        assertThat(fields).isNotEmpty();
        assertThat(columns).allSatisfy(column -> assertNoSynonym(column, segmentsOfColumn(column)));
        assertThat(fields).allSatisfy(field -> assertNoSynonym(field,
                segmentsOfField(field.substring(field.lastIndexOf('.') + 1))));
    }

    private static void assertNoSynonym(String owner, List<String> segments) {
        segments.forEach(segment -> assertThat(BANNED_ANYWHERE)
                .describedAs("%s abbreviates %s: one concept is spelled one way across the schema",
                        owner, BANNED_ANYWHERE.get(segment))
                .doesNotContainKey(segment));
        String last = segments.getLast();
        assertThat(BANNED_AS_UNIT)
                .describedAs("%s abbreviates the unit %s, and a unit that has to be guessed is a unit "
                        + "somebody will guess wrong", owner, BANNED_AS_UNIT.get(last))
                .doesNotContainKey(last);
    }

    private static boolean isEnumConstant(JavaField field) {
        return field.getOwner().isEnum() && field.getRawType().equals(field.getOwner());
    }

    private static List<String> segmentsOfColumn(String column) {
        return List.of(column.split("_"));
    }

    private static List<String> segmentsOfField(String field) {
        return List.of(field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase().split("_"));
    }
}
