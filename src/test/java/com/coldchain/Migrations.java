package com.coldchain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class Migrations {

    static final Path FOLDER = Path.of("src", "main", "resources", "db", "migration");

    private static final Pattern FILE_NAME = Pattern.compile("V([0-9]+)__([a-z0-9_]+)[.]sql");

    private static final Pattern CREATE_TABLE =
            Pattern.compile("CREATE TABLE ([A-Z0-9_]+)\\s*\\((.*?)\\n\\);", Pattern.DOTALL);

    private static final Pattern CREATE_INDEX =
            Pattern.compile("CREATE (UNIQUE )?INDEX ([A-Z0-9_]+) ON ([A-Z0-9_]+)");

    private static final Pattern CONSTRAINT = Pattern.compile("CONSTRAINT ([A-Z0-9_]+)");

    private Migrations() {
    }

    record Script(int version, String name, Path file, String text) {
    }

    record Table(String name, String body, Script script) {

        List<String> constraintNames() {
            List<String> names = new ArrayList<>();
            Matcher matcher = CONSTRAINT.matcher(body);
            while (matcher.find()) {
                names.add(matcher.group(1));
            }
            return names;
        }

        String bodyWithoutConstraintNames() {
            return CONSTRAINT.matcher(body).replaceAll("CONSTRAINT");
        }
    }

    record Index(String name, String table, boolean unique) {
    }

    static List<Script> scripts() {
        try (Stream<Path> paths = Files.list(FOLDER)) {
            return paths
                    .filter(path -> FILE_NAME.matcher(path.getFileName().toString()).matches())
                    .map(Migrations::toScript)
                    .sorted(Comparator.comparingInt(Script::version))
                    .toList();
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not list " + FOLDER, cause);
        }
    }

    static List<Table> tables() {
        List<Table> tables = new ArrayList<>();
        for (Script script : scripts()) {
            Matcher matcher = CREATE_TABLE.matcher(script.text());
            while (matcher.find()) {
                tables.add(new Table(matcher.group(1), matcher.group(2), script));
            }
        }
        return List.copyOf(tables);
    }

    static List<Index> indexes() {
        List<Index> indexes = new ArrayList<>();
        for (Script script : scripts()) {
            Matcher matcher = CREATE_INDEX.matcher(script.text());
            while (matcher.find()) {
                indexes.add(new Index(matcher.group(2), matcher.group(3), matcher.group(1) != null));
            }
        }
        return List.copyOf(indexes);
    }

    private static Script toScript(Path file) {
        Matcher matcher = FILE_NAME.matcher(file.getFileName().toString());
        if (!matcher.matches()) {
            throw new IllegalStateException(file + " is not a Flyway script");
        }
        try {
            return new Script(Integer.parseInt(matcher.group(1)), matcher.group(2), file,
                    Files.readString(file));
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not read " + file, cause);
        }
    }
}
