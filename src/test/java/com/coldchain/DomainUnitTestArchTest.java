package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainUnitTestArchTest {

    private static final List<String> INFRASTRUCTURE = List.of(
            "org.springframework", "org.testcontainers", "jakarta.persistence", "javax.sql");

    @Test
    void everyDomainServiceIsExercisedOnItsOwn() {
        List<Path> services = SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(file -> SourceTree.packageOf(file).endsWith(".internal.domain.service"))
                .filter(file -> SourceTree.read(file).contains("public final class"))
                .toList();

        assertThat(services)
                .describedAs("the pure calculators are what the hexagon is for")
                .isNotEmpty();
        assertThat(services).allSatisfy(service -> {
            String name = service.getFileName().toString().replace(".java", "");
            assertThat(SourceTree.find(name + "Test", SourceTree.TEST))
                    .describedAs("%s carries a business rule and nothing exercises it without a "
                            + "database, which is the benefit the whole hexagon is paid for", name)
                    .isPresent();
        });
    }

    @Test
    void theDomainSuiteNeedsNeitherSpringNorADatabase() {
        List<Path> domainTests = SourceTree.javaFiles(SourceTree.TEST).stream()
                .filter(file -> SourceTree.packageOf(file).startsWith("com.coldchain.modules"))
                .toList();

        assertThat(domainTests)
                .describedAs("the domain is unit-tested somewhere")
                .isNotEmpty();
        assertThat(domainTests).allSatisfy(test -> assertThat(INFRASTRUCTURE)
                .allSatisfy(framework -> assertThat(SourceTree.read(test))
                        .describedAs("%s reaches for %s, and a domain test that needs a framework "
                                + "is measuring the framework", test.getFileName(), framework)
                        .doesNotContain("import " + framework)));
    }

    @Test
    void theStateMachineAndTheVerdictAreCoveredByName() {
        List<String> whatTheReadmePromises =
                List.of("StatusTransitionsTest", "VerdictCalculatorTest", "ExcursionDetectorTest");

        assertThat(whatTheReadmePromises).allSatisfy(test ->
                assertThat(SourceTree.find(test, SourceTree.TEST))
                        .describedAs("the README names this rule as the reason the domain is pure, "
                                + "so deleting %s makes the README a lie", test)
                        .isPresent());
    }
}
