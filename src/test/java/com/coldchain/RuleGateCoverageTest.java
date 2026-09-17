package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.RuleCatalogue.Rule;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RuleGateCoverageTest {

    private static final Path BUILD_FILE = Path.of("build.gradle");

    private static final Path WORKFLOW = Path.of(".github", "workflows", "ci.yml");

    private static final String GATE_PACKAGE = "com.coldchain";

    private static final List<String> WAYS_TO_GO_GREEN_WITHOUT_CHECKING =
            List.of("-x test", "--continue", "continue-on-error", "-x rules");

    private static final List<String> GATE_BLOCKS =
            List.of("tasks.register('rules', Test)", "tasks.register('rulesDb', Test)");

    private static final List<String> WAYS_TO_TURN_A_TASK_OFF =
            List.of("onlyIf", "enabled =", "System.getenv", "findProperty");

    private static final String CONCESSION = "allowEmptyShould(" + "true)";

    private final List<Rule> rules = RuleCatalogue.rules();

    private final String buildFile = SourceTree.read(BUILD_FILE);

    private final String workflow = SourceTree.read(WORKFLOW);

    @Test
    void somethingChecksThatTheRulesActuallyRan() {
        assertThat(buildFile)
                .describedAs("a suite cannot prove it ran, so the build reads the results afterwards")
                .contains("registerRanCheck('rulesRan', 'rules', false)")
                .contains("registerRanCheck('rulesDbRan', 'rulesDb', true)")
                .contains("finalizedBy 'rulesRan'")
                .contains("finalizedBy 'rulesDbRan'");
    }

    @Test
    void everyClassThatDeclaresRulesLivesWhereTheGateLooks() {
        assertThat(buildFile)
                .describedAs("the gate selects by location, not by a list of classes")
                .contains("include 'com/coldchain/*.class'");

        List<String> misplaced = citedEnforcerClasses().stream()
                .map(className -> SourceTree.find(className, SourceTree.TEST, SourceTree.INTEGRATION_TEST))
                .flatMap(Optional::stream)
                .filter(source -> !SourceTree.packageOf(source).equals(GATE_PACKAGE))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(misplaced)
                .describedAs("a rule written outside %s is one continuous integration never runs", GATE_PACKAGE)
                .isEmpty();
    }

    @Test
    void continuousIntegrationRunsTheGateAndCannotSkipIt() {
        assertThat(workflow)
                .contains("./gradlew rules")
                .contains("push:")
                .contains("pull_request:");
    }

    @Test
    void theGateHasNoBackDoor() {
        assertThat(WAYS_TO_GO_GREEN_WITHOUT_CHECKING)
                .allSatisfy(shortcut -> assertThat(workflow)
                        .describedAs("the workflow must not carry %s", shortcut)
                        .doesNotContain(shortcut));

        assertThat(GATE_BLOCKS).allSatisfy(marker -> {
            String block = SourceTree.blockAt(buildFile, marker);
            assertThat(WAYS_TO_TURN_A_TASK_OFF).allSatisfy(switchOff -> assertThat(block)
                    .describedAs("%s must not be switchable with %s", marker, switchOff)
                    .doesNotContain(switchOff));
        });
    }

    @Test
    void noRuleIsAllowedToPassOnAnEmptySelector() {
        List<String> conceding = List.of(SourceTree.TEST, SourceTree.INTEGRATION_TEST).stream()
                .flatMap(root -> SourceTree.javaFiles(root).stream())
                .filter(source -> SourceTree.read(source).contains(CONCESSION))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(conceding)
                .describedAs("\"I found nothing to look at\" is not the same as green")
                .isEmpty();
    }

    @Test
    void everyRuleThatNeedsOracleIsDeclared() {
        List<String> misdeclared = citedEnforcerClasses().stream()
                .filter(className -> {
                    boolean needsOracle = className.endsWith("IT");
                    Path expected = needsOracle ? SourceTree.INTEGRATION_TEST : SourceTree.TEST;
                    return SourceTree.find(className, expected).isEmpty()
                            && SourceTree.find(className, SourceTree.TEST, SourceTree.INTEGRATION_TEST).isPresent();
                })
                .toList();

        assertThat(misdeclared)
                .describedAs("the suffix decides the gate: *IT runs in rulesDb and the rest in rules")
                .isEmpty();
    }

    private List<String> citedEnforcerClasses() {
        return rules.stream()
                .filter(Rule::declaresAMachine)
                .flatMap(rule -> rule.citedEnforcers().stream())
                .map(enforcer -> enforcer.contains(".") ? enforcer.substring(0, enforcer.indexOf('.')) : enforcer)
                .distinct()
                .toList();
    }
}
