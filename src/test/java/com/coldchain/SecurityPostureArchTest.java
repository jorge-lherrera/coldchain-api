package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.identity.api.Scope;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class SecurityPostureArchTest {

    private static final String ROOT = "com.coldchain";

    private static final Path SECURITY_CONFIG = SourceTree.MAIN
            .resolve(Path.of("com", "coldchain", "shared", "security", "SecurityConfig.java"));

    private static final Path RESOURCES = Path.of("src", "main", "resources");

    private static final String SCOPE_PREFIX = "SCOPE_";

    private static final String PARTICIPATION_PREDICATE = "FROM ShipmentParticipantJpaEntity";

    private static final List<String> FAST_OR_REVERSIBLE_HASHES =
            List.of("BCryptPasswordEncoder", "NoOpPasswordEncoder", "SCryptPasswordEncoder",
                    "Pbkdf2PasswordEncoder", "StandardPasswordEncoder", "MD5", "SHA-1");

    private static final List<String> SECRET_BEARING_KEYS =
            List.of("password", "secret", "jwt-secret", "client-secret", "private-key");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    private final String securityConfig = SourceTree.read(SECURITY_CONFIG);

    @Test
    void thePublicRoutesAreDeclaredInOnePlace() {
        List<String> elsewhere = SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(source -> !source.endsWith(SECURITY_CONFIG.getFileName()))
                .filter(source -> SourceTree.read(source).contains("permitAll"))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(elsewhere)
                .describedAs("a route opened away from the filter chain is one nobody can audit")
                .isEmpty();
        assertThat(publicRoutes())
                .describedAs("the filter chain declares the routes that need no credentials")
                .isNotEmpty();
    }

    @Test
    void everyEndpointIsScopeGuarded() {
        List<String> unguarded = new ArrayList<>();
        List<String> guardedButPublic = new ArrayList<>();

        endpoints().forEach(endpoint -> {
            boolean guarded = endpoint.method().isAnnotatedWith(PreAuthorize.class);
            if (publicRoutes().contains(endpoint.route()) && guarded) {
                guardedButPublic.add(endpoint.describe());
            }
            if (!publicRoutes().contains(endpoint.route()) && !guarded) {
                unguarded.add(endpoint.describe());
            }
        });

        assertThat(endpoints()).describedAs("there are endpoints to guard").isNotEmpty();
        assertThat(unguarded)
                .describedAs("an endpoint that is neither public nor annotated is open by omission")
                .isEmpty();
        assertThat(guardedButPublic)
                .describedAs("a route cannot be public in the chain and guarded on the method")
                .isEmpty();
    }

    @Test
    void everyScopeDemandedByAnEndpointExistsInTheCatalogue() {
        Set<String> catalogue = Arrays.stream(Scope.values()).map(Enum::name)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));

        List<String> demanded = endpoints().stream()
                .filter(endpoint -> endpoint.method().isAnnotatedWith(PreAuthorize.class))
                .map(endpoint -> endpoint.method().getAnnotationOfType(PreAuthorize.class).value())
                .flatMap(expression -> scopesIn(expression).stream())
                .distinct()
                .toList();

        assertThat(demanded).describedAs("at least one endpoint demands a scope").isNotEmpty();
        assertThat(catalogue)
                .describedAs("an endpoint demands a permission that the closed catalogue does not define")
                .containsAll(demanded);
    }

    @Test
    void theFilterChainIsStatelessAndDropsCsrf() {
        assertThat(securityConfig)
                .describedAs("with no server-side session there is nothing for CSRF to forge")
                .contains("SessionCreationPolicy.STATELESS")
                .contains("csrf(AbstractHttpConfigurer::disable)")
                .contains("httpBasic(AbstractHttpConfigurer::disable)")
                .contains("formLogin(AbstractHttpConfigurer::disable)");
    }

    @Test
    void endpointsDoNotTakeUserContextHeaders() {
        List<String> readingHeaders = SourceTree
                .javaFiles(SourceTree.MAIN.resolve(Path.of("com", "coldchain", "delivery"))).stream()
                .filter(source -> SourceTree.read(source).contains("@RequestHeader"))
                .map(source -> source.getFileName().toString())
                .toList();

        List<String> adaptersReadingTheActor = SourceTree
                .javaFiles(SourceTree.MAIN.resolve(Path.of("com", "coldchain", "modules"))).stream()
                .filter(source -> source.toString().contains("infrastructure"))
                .filter(source -> SourceTree.read(source).contains("CurrentActor")
                        || SourceTree.read(source).contains("SecurityContextHolder"))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(readingHeaders)
                .describedAs("who the caller is travels signed in the token, never in a header they pick")
                .isEmpty();
        assertThat(adaptersReadingTheActor)
                .describedAs("an adapter that reads the caller decides authorization where nobody reviews it")
                .isEmpty();
    }

    @Test
    void secretsAreHashedWithArgon2() {
        List<Path> hashers = SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(source -> SourceTree.read(source).contains("implements SecretHasher"))
                .toList();

        assertThat(hashers).describedAs("something implements the hashing port").hasSize(1);
        assertThat(SourceTree.read(hashers.getFirst()))
                .describedAs("a password is hashed with Argon2id, never with anything reversible or fast")
                .contains("Argon2PasswordEncoder")
                .contains("defaultsForSpringSecurity");

        List<String> weak = SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(source -> FAST_OR_REVERSIBLE_HASHES.stream()
                        .anyMatch(hash -> SourceTree.read(source).contains(hash)))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(weak).describedAs("a fast or reversible hash is not a way to store a secret").isEmpty();
    }

    @Test
    void aMachineClientCannotAskForATenant() {
        List<String> askingForATenant = SourceTree
                .javaFiles(SourceTree.MAIN.resolve(Path.of("com", "coldchain", "delivery"))).stream()
                .filter(source -> source.toString().contains("dto"))
                .filter(source -> SourceTree.read(source).contains("Request"))
                .filter(source -> SourceTree.read(source).contains("organizationId"))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(askingForATenant)
                .describedAs("the organization travels signed in the token, so no request body may name one")
                .isEmpty();

        Path useCase = SourceTree.find("CreateApiClientUseCase", SourceTree.MAIN).orElseThrow();
        assertThat(SourceTree.read(useCase))
                .describedAs("a machine credential carries the ingestion scope and nothing else")
                .contains("ALLOWED_FOR_MACHINES = Set.of(Scope.TELEMETRY_INGEST)");
    }

    @Test
    void theUnauthenticatedEndpointsAreRateLimited() {
        assertThat(securityConfig)
                .describedAs("the limited routes are the public ones, so a new public route gets a limit")
                .contains("new RateLimitFilter(publicRouteMatcher()")
                .contains("PUBLIC_ROUTES.stream()");

        String matcher = SourceTree.blockAt(securityConfig, "private static RequestMatcher publicRouteMatcher()");
        assertThat(matcher)
                .describedAs("the limiter must not be pointed at a list of its own")
                .contains("PUBLIC_ROUTES");
    }

    @Test
    void theTokenLifetimesAreFifteenMinutesAndSevenDays() {
        String configuration = SourceTree.read(RESOURCES.resolve("application.yml"));

        assertThat(configuration)
                .describedAs("a short access token is what makes a stolen one worth little")
                .contains("access-token-lifetime: 15m")
                .contains("refresh-token-lifetime: 7d");
    }

    @Test
    void everySecurityHeaderIsDeclared() {
        assertThat(securityConfig)
                .describedAs("the headers a browser-facing API cannot ship without")
                .contains("default-src 'none'")
                .contains("frame-ancestors 'none'")
                .contains("31_536_000")
                .contains("ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER")
                .contains("permissionsPolicyHeader")
                .contains("frameOptions");
    }

    @Test
    void noSecretIsWrittenIntoTheConfiguration() {
        List<String> literals = configurationLines().stream()
                .filter(line -> SECRET_BEARING_KEYS.stream()
                        .anyMatch(key -> line.trim().toLowerCase(java.util.Locale.ROOT)
                                .startsWith(key + ":")))
                .filter(line -> !line.contains("${"))
                .map(String::trim)
                .toList();

        assertThat(literals)
                .describedAs("a secret in a committed file is a secret everybody with the repository holds")
                .isEmpty();
        assertThat(configurationLines().stream().anyMatch(line -> line.contains("${")))
                .describedAs("the configuration reads its secrets from the environment")
                .isTrue();
    }

    private static List<String> configurationLines() {
        try (Stream<Path> files = Files.walk(RESOURCES)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".yml")
                            || file.getFileName().toString().endsWith(".properties"))
                    .flatMap(file -> SourceTree.read(file).lines())
                    .toList();
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not walk " + RESOURCES, cause);
        }
    }

    private List<Endpoint> endpoints() {
        List<Endpoint> endpoints = new ArrayList<>();
        production.stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(RestController.class))
                .forEach(controller -> {
                    String base = controller.isAnnotatedWith(RequestMapping.class)
                            ? firstOrEmpty(controller.getAnnotationOfType(RequestMapping.class).value())
                            : "";
                    controller.getMethods().stream()
                            .filter(SecurityPostureArchTest::isHandler)
                            .forEach(method -> endpoints.add(
                                    new Endpoint(controller, method, base + pathOf(method))));
                });
        return endpoints;
    }

    private Set<String> publicRoutes() {
        return quotedValuesOf("PUBLIC_ROUTES");
    }

    private Set<String> quotedValuesOf(String constant) {
        int start = securityConfig.indexOf(constant + " = List.of(");
        int end = securityConfig.indexOf(");", start);
        Set<String> values = new TreeSet<>();
        String block = securityConfig.substring(start, end);
        int quote = block.indexOf('"');
        while (quote >= 0) {
            int close = block.indexOf('"', quote + 1);
            values.add(block.substring(quote + 1, close));
            quote = block.indexOf('"', close + 1);
        }
        return values;
    }

    private static boolean isHandler(JavaMethod method) {
        return method.isAnnotatedWith(GetMapping.class) || method.isAnnotatedWith(PostMapping.class)
                || method.isAnnotatedWith(PutMapping.class) || method.isAnnotatedWith(PatchMapping.class)
                || method.isAnnotatedWith(DeleteMapping.class)
                || method.isAnnotatedWith(RequestMapping.class);
    }

    private static String pathOf(JavaMethod method) {
        if (method.isAnnotatedWith(GetMapping.class)) {
            return firstOrEmpty(method.getAnnotationOfType(GetMapping.class).value());
        }
        if (method.isAnnotatedWith(PostMapping.class)) {
            return firstOrEmpty(method.getAnnotationOfType(PostMapping.class).value());
        }
        if (method.isAnnotatedWith(PutMapping.class)) {
            return firstOrEmpty(method.getAnnotationOfType(PutMapping.class).value());
        }
        if (method.isAnnotatedWith(PatchMapping.class)) {
            return firstOrEmpty(method.getAnnotationOfType(PatchMapping.class).value());
        }
        if (method.isAnnotatedWith(DeleteMapping.class)) {
            return firstOrEmpty(method.getAnnotationOfType(DeleteMapping.class).value());
        }
        return firstOrEmpty(method.getAnnotationOfType(RequestMapping.class).value());
    }

    private static String firstOrEmpty(String[] values) {
        return values.length == 0 ? "" : values[0];
    }

    private static List<String> scopesIn(String expression) {
        List<String> scopes = new ArrayList<>();
        int index = expression.indexOf(SCOPE_PREFIX);
        while (index >= 0) {
            int end = index + SCOPE_PREFIX.length();
            while (end < expression.length()
                    && (Character.isUpperCase(expression.charAt(end)) || expression.charAt(end) == '_')) {
                end++;
            }
            scopes.add(expression.substring(index + SCOPE_PREFIX.length(), end));
            index = expression.indexOf(SCOPE_PREFIX, end);
        }
        return scopes;
    }

    @Test
    void shipmentVisibilityIsResolvedInOnePlace() {
        List<Path> declaring = SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(file -> SourceTree.read(file).contains(PARTICIPATION_PREDICATE))
                .toList();

        assertThat(declaring)
                .describedAs("visibility of a shipment is participation, and it is decided once: "
                        + "a second copy of the predicate is a query that will forget it")
                .hasSize(1);
        assertThat(declaring.getFirst().toString())
                .describedAs("the predicate lives in the repository, where a new query cannot get "
                        + "around it, and not in a service somebody can bypass")
                .contains("persistence")
                .endsWith("ShipmentJpaRepository.java");
    }

    @Test
    void everyDenialIsAudited() {
        String responder = SourceTree.read(SourceTree.find("ProblemErrorResponder", SourceTree.MAIN)
                .orElseThrow());

        assertThat(responder)
                .describedAs("a call with no credentials and a call with the wrong scope are two "
                        + "different facts, and the log has to tell them apart")
                .contains("LogMessages.DENIED_UNAUTHENTICATED")
                .contains("LogMessages.DENIED_SCOPE");
        assertThat(responder)
                .describedAs("a denial leaves as a translated problem document, not as an empty body")
                .contains("problemDetails.describe")
                .contains("APPLICATION_PROBLEM_JSON_VALUE");
    }

    private record Endpoint(JavaClass controller, JavaMethod method, String route) {

        String describe() {
            return controller.getSimpleName() + "." + method.getName() + " -> " + route;
        }
    }
}
