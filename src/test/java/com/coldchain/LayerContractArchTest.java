package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LayerContractArchTest {

    private static final Path MODULES = SourceTree.MAIN.resolve(Path.of("com", "coldchain", "modules"));

    private static final Path DELIVERY = SourceTree.MAIN.resolve(Path.of("com", "coldchain", "delivery"));

    private static final Path DOCS = Path.of("docs");

    private static final List<String> DECLARED_MODULES =
            List.of("identity", "catalog", "shipment", "telemetry", "compliance");

    private static final List<String> WRITING_VERBS =
            List.of(".save(", ".delete(", ".grant(", ".revoke(", "publishEvent(");

    private static final List<String> OUTBOUND_CLIENTS =
            List.of("RestClient", "WebClient", "RestTemplate", "HttpClient", "URLConnection");

    @Test
    void queriesDoNotWrite() {
        List<Path> queries = queryUseCases();

        assertThat(queries).describedAs("there are queries to check").isNotEmpty();
        assertThat(queries).allSatisfy(source -> assertThat(WRITING_VERBS)
                .allSatisfy(verb -> assertThat(SourceTree.read(source))
                        .describedAs("%s lives under query/ and must not %s", source.getFileName(), verb)
                        .doesNotContain(verb)));
    }

    @Test
    void queriesAreReadOnlyWhenTransactional() {
        List<String> writingTransactions = queryUseCases().stream()
                .filter(source -> SourceTree.read(source).contains("Transactional"))
                .filter(source -> !SourceTree.read(source).contains("readOnly = true"))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(writingTransactions)
                .describedAs("a read that opens a writable transaction takes locks it never needed")
                .isEmpty();
    }

    @Test
    void everyRequestBodyIsValidated() {
        List<String> unvalidated = controllers().stream()
                .filter(source -> {
                    String text = SourceTree.read(source);
                    long bodies = occurrences(text, "@RequestBody");
                    long validated = occurrences(text, "@Valid @RequestBody");
                    return bodies != validated;
                })
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(controllers()).describedAs("there are controllers to check").isNotEmpty();
        assertThat(unvalidated)
                .describedAs("a body that is not validated at the edge reaches the use case malformed")
                .isEmpty();
    }

    @Test
    void noBusinessRuleIsDecidedAtTheEdge() {
        List<String> deciding = SourceTree.javaFiles(DELIVERY).stream()
                .filter(source -> {
                    String text = SourceTree.read(source);
                    return text.contains("DomainException") || text.contains("ErrorCode.");
                })
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(deciding)
                .describedAs("the edge validates shape; the use case decides meaning, and only one of them "
                        + "can be the place where a rule lives")
                .isEmpty();
    }

    @Test
    void everyEventLivesInTheModulesApi() {
        List<String> published = SourceTree.javaFiles(MODULES).stream()
                .flatMap(source -> publishedEventTypes(SourceTree.read(source)).stream())
                .distinct()
                .sorted()
                .toList();

        assertThat(published).describedAs("a module publishes at least one fact").isNotEmpty();

        List<String> hidden = published.stream()
                .filter(type -> DECLARED_MODULES.stream()
                        .noneMatch(module -> Files.isRegularFile(
                                MODULES.resolve(module).resolve("api").resolve("event")
                                        .resolve(type + ".java"))))
                .toList();

        assertThat(hidden)
                .describedAs("an event is part of the contract, so it lives in api/event and nowhere else")
                .isEmpty();
    }

    @Test
    void noExternalCallRunsInsideATransaction() {
        List<Path> transactional = SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(source -> SourceTree.read(source).contains("Transactional"))
                .toList();

        assertThat(transactional).describedAs("there are transactions to check").isNotEmpty();

        List<String> callingOut = transactional.stream()
                .filter(source -> OUTBOUND_CLIENTS.stream()
                        .anyMatch(client -> SourceTree.read(source).contains(client)))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(callingOut)
                .describedAs("a transaction that waits for a third party holds its locks for as long "
                        + "as the third party takes")
                .isEmpty();
    }

    @Test
    void everyModuleHasItsBuildPlan() {
        assertThat(DECLARED_MODULES).allSatisfy(module -> {
            List<Path> plans = plansFor(module);
            assertThat(plans)
                    .describedAs("module %s has no docs/plan-0N-%s.md", module, module)
                    .hasSize(1);
            assertThat(SourceTree.read(plans.getFirst()))
                    .describedAs("the plan for %s must close on something somebody can run", module)
                    .contains("**Milestone done when:**")
                    .contains("`");
        });
    }

    private static List<Path> plansFor(String module) {
        try (Stream<Path> entries = Files.list(DOCS)) {
            return entries
                    .filter(path -> path.getFileName().toString().startsWith("plan-"))
                    .filter(path -> path.getFileName().toString().endsWith("-" + module + ".md"))
                    .toList();
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not list " + DOCS, cause);
        }
    }

    private static List<String> publishedEventTypes(String source) {
        List<String> types = new ArrayList<>();
        String marker = "publishEvent(new ";
        int index = source.indexOf(marker);
        while (index >= 0) {
            int start = index + marker.length();
            int end = start;
            while (end < source.length() && Character.isJavaIdentifierPart(source.charAt(end))) {
                end++;
            }
            types.add(source.substring(start, end));
            index = source.indexOf(marker, end);
        }
        return types;
    }

    private static long occurrences(String text, String needle) {
        long found = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            found++;
            index = text.indexOf(needle, index + needle.length());
        }
        return found;
    }

    private List<Path> queryUseCases() {
        return SourceTree.javaFiles(MODULES).stream()
                .filter(source -> source.toString().contains("usecase")
                        && source.toString().contains("query"))
                .toList();
    }

    private List<Path> controllers() {
        return SourceTree.javaFiles(DELIVERY).stream()
                .filter(source -> SourceTree.read(source).contains("@RestController"))
                .toList();
    }
}
