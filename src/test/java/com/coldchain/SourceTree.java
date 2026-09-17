package com.coldchain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

final class SourceTree {

    static final Path MAIN = Path.of("src", "main", "java");

    static final Path TEST = Path.of("src", "test", "java");

    static final Path INTEGRATION_TEST = Path.of("src", "integrationTest", "java");

    private SourceTree() {
    }

    static Optional<Path> find(String simpleClassName, Path... roots) {
        for (Path root : roots) {
            Optional<Path> found = walk(root)
                    .filter(path -> path.getFileName().toString().equals(simpleClassName + ".java"))
                    .findFirst();
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    static List<Path> javaFiles(Path root) {
        return walk(root).toList();
    }

    static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not read " + file, cause);
        }
    }

    static String packageOf(Path file) {
        return read(file).lines()
                .filter(line -> line.startsWith("package "))
                .map(line -> line.substring("package ".length()).replace(";", "").trim())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(file + " declares no package"));
    }

    private static Stream<Path> walk(Path root) {
        if (!Files.isDirectory(root)) {
            return Stream.empty();
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".java")).toList().stream();
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not walk " + root, cause);
        }
    }
}
