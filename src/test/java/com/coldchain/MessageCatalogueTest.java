package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MessageCatalogueTest {

    private static final Path RESOURCES = Path.of("src", "main", "resources");

    private static final List<String> LANGUAGES = List.of("en", "es", "pt");

    private static final Pattern DECLARATION =
            Pattern.compile("\"((?:error|success)[.][a-z0-9_.]+)\"\\s*,\\s*[A-Za-z_.]+\\s*,\\s*\"([^\"]+)\"");

    @Test
    void everyKeyTheCodeCarriesIsWrittenInEveryLanguage() {
        Map<String, String> declared = declaredKeys();
        assertThat(declared).isNotEmpty();

        assertThat(LANGUAGES).allSatisfy(language -> {
            Set<String> written = keysOf(language);
            assertThat(written)
                    .describedAs("the %s catalogue is missing keys the code answers with, so those "
                            + "responses fall back to English without anybody noticing", language)
                    .containsAll(declared.keySet());
        });
    }

    @Test
    void theCatalogueCarriesNoKeyTheCodeNoLongerUses() {
        Set<String> declared = declaredKeys().keySet();

        assertThat(LANGUAGES).allSatisfy(language -> {
            Set<String> orphans = new TreeSet<>(keysOf(language));
            orphans.removeAll(declared);
            assertThat(orphans)
                    .describedAs("the %s catalogue answers keys nothing asks for any more", language)
                    .isEmpty();
        });
    }

    @Test
    void theEnglishCatalogueSaysWhatTheCodeSays() {
        Map<String, String> declared = declaredKeys();
        Map<String, String> english = valuesOf("en");

        assertThat(declared).allSatisfy((key, message) -> assertThat(english.get(key))
                .describedAs("%s reads differently in the code and in the catalogue, so the "
                        + "response depends on which one answered", key)
                .isEqualTo(message));
    }

    @Test
    void everyModuleKeepsItsOwnMessagesAndTheSharedOnesAreOnlyTheCoreOnes() {
        assertThat(keysOf("en", RESOURCES.resolve("i18n")))
                .describedAs("a module message in the shared catalogue is a module leaking upwards")
                .allSatisfy(key -> assertThat(key).startsWith("error.core."));

        assertThat(bundles(RESOURCES.resolve("modules"), "en")).isNotEmpty();
        assertThat(bundles(RESOURCES.resolve("modules"), "en")).allSatisfy(bundle -> {
            String module = bundle.getParent().getParent().getFileName().toString();
            assertThat(read(bundle).stringPropertyNames())
                    .describedAs("%s carries messages that do not belong to its module", bundle)
                    .allSatisfy(key -> assertThat(key.split("[.]")[1]).isEqualTo(module));
        });
    }

    private Map<String, String> declaredKeys() {
        Map<String, String> declared = new TreeMap<>();
        SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(file -> file.getFileName().toString().endsWith("ErrorCode.java")
                        || file.getFileName().toString().endsWith("SuccessCode.java"))
                .map(SourceTree::read)
                .forEach(text -> {
                    Matcher matcher = DECLARATION.matcher(text);
                    while (matcher.find()) {
                        declared.put(matcher.group(1), matcher.group(2));
                    }
                });
        return declared;
    }

    private Set<String> keysOf(String language) {
        Set<String> keys = new TreeSet<>(keysOf(language, RESOURCES.resolve("i18n")));
        keys.addAll(keysOf(language, RESOURCES.resolve("modules")));
        return keys;
    }

    private Set<String> keysOf(String language, Path root) {
        Set<String> keys = new TreeSet<>();
        bundles(root, language).forEach(bundle -> keys.addAll(read(bundle).stringPropertyNames()));
        keys.removeIf(key -> key.startsWith("jakarta."));
        return keys;
    }

    private Map<String, String> valuesOf(String language) {
        Map<String, String> values = new TreeMap<>();
        Stream.of(RESOURCES.resolve("i18n"), RESOURCES.resolve("modules"))
                .flatMap(root -> bundles(root, language).stream())
                .forEach(bundle -> read(bundle).forEach((key, value) ->
                        values.put(key.toString(), value.toString())));
        return values;
    }

    private List<Path> bundles(Path root, String language) {
        try (Stream<Path> tree = Files.walk(root)) {
            return tree.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith("_" + language + ".properties"))
                    .sorted()
                    .toList();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("Could not walk " + root, unreadable);
        }
    }

    private Properties read(Path bundle) {
        Properties properties = new Properties();
        try {
            properties.load(Files.newBufferedReader(bundle, StandardCharsets.UTF_8));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("Could not read " + bundle, unreadable);
        }
        return properties;
    }
}
