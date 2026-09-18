package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ObservabilityArchTest {

    private static final Path CONFIGURATION = Path.of("src", "main", "resources", "application.yml");

    private static final Path CATALOGUE = SourceTree.MAIN.resolve(
            Path.of("com", "coldchain", "shared", "observability", "LogMessage.java"));

    private static final Path TRACE = SourceTree.MAIN.resolve(
            Path.of("com", "coldchain", "shared", "trace", "RequestTrace.java"));

    private static final Path SECURITY = SourceTree.MAIN.resolve(
            Path.of("com", "coldchain", "shared", "security", "SecurityConfig.java"));

    private static final Pattern LOG_CALL =
            Pattern.compile("LOG[.](trace|debug|info|warn|error)\\s*\\(([^;]*)\\);", Pattern.DOTALL);

    private static final Pattern EMPTY_CATCH =
            Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}");

    private static final List<String> NAMES_OF_WHAT_MUST_NOT_BE_LOGGED = List.of(
            "email", "password", "secret", "token", "taxId", "hash", "credential", "payload");

    private static final List<String> ACTUATOR_ENDPOINTS_EXPOSED =
            List.of("health", "info", "metrics");

    @Test
    void nothingSensitiveIsLogged() {
        List<JavaSource.Scan> sources = JavaSource.scanAll(SourceTree.MAIN);

        assertThat(sources).isNotEmpty();
        assertThat(logCallsOf(sources))
                .describedAs("a log line that carries personal data outlives the request, the "
                        + "retention policy and the person who wrote it")
                .allSatisfy(call -> assertThat(NAMES_OF_WHAT_MUST_NOT_BE_LOGGED)
                        .noneSatisfy(banned -> assertThat(call.toLowerCase())
                                .contains(banned.toLowerCase())));
    }

    @Test
    void everyLogCarriesTheTraceId() {
        assertThat(SourceTree.read(CONFIGURATION))
                .describedAs("a log nobody can correlate is read by eye, one layer at a time")
                .contains("structured:")
                .contains("console: ecs");
        assertThat(SourceTree.read(TRACE))
                .describedAs("the trace identifier reaches the log through the diagnostic context, "
                        + "which is what the structured format writes on every line")
                .contains("MDC.put");
    }

    @Test
    void errorLevelIsReservedForTheActionable() {
        List<JavaSource.Scan> sources = JavaSource.scanAll(SourceTree.MAIN).stream()
                .filter(source -> !source.file().getFileName().toString()
                        .equals("GlobalExceptionHandler.java"))
                .toList();

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(source -> assertThat(source.code())
                .describedAs("%s raises an ERROR nobody is obliged to attend, and an ERROR nobody "
                        + "attends teaches people not to look", source.file())
                .doesNotContain("LOG.error("));
    }

    @Test
    void noCatchBlockIsEmpty() {
        List<JavaSource.Scan> sources = JavaSource.scanAll(SourceTree.MAIN);

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(source -> assertThat(
                EMPTY_CATCH.matcher(source.code()).find())
                .describedAs("%s swallows an exception, which turns a failure into a wrong value",
                        source.file())
                .isFalse());
    }

    @Test
    void logMessagesComeFromOneCatalogue() {
        List<JavaSource.Scan> sources = JavaSource.scanAll(SourceTree.MAIN);
        List<String> calls = logCallsOf(sources);

        assertThat(calls).isNotEmpty();
        assertThat(calls).allSatisfy(call -> assertThat(call.trim())
                .describedAs("a message written at the call site cannot be found, counted or "
                        + "changed anywhere else")
                .startsWith("LogMessage."));
        assertThat(SourceTree.read(CATALOGUE))
                .describedAs("the catalogue is where the messages live")
                .contains("public static final String");
    }

    @Test
    void onlyHealthIsPublicAmongTheActuatorEndpoints() {
        String configuration = SourceTree.read(CONFIGURATION);
        String security = SourceTree.read(SECURITY);

        assertThat(configuration)
                .describedAs("health and metrics are exposed on purpose and the list says which")
                .contains("include: " + String.join(",", ACTUATOR_ENDPOINTS_EXPOSED));
        assertThat(security)
                .describedAs("the probe answers without credentials because a probe cannot carry any")
                .contains("EndpointRequest.to(\"health\")");
        assertThat(ACTUATOR_ENDPOINTS_EXPOSED.stream()
                .filter(endpoint -> !endpoint.equals("health"))
                .filter(endpoint -> security.contains("EndpointRequest.to(\"" + endpoint + "\")"))
                .toList())
                .describedAs("an actuator endpoint other than health answers without credentials")
                .isEmpty();
    }

    private static List<String> logCallsOf(List<JavaSource.Scan> sources) {
        return sources.stream()
                .flatMap(source -> {
                    Matcher matcher = LOG_CALL.matcher(source.code());
                    return matcher.results().map(result -> result.group(2));
                })
                .toList();
    }
}
