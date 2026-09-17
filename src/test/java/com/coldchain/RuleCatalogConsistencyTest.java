package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.RuleCatalogue.Rule;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RuleCatalogConsistencyTest {

    private static final Pattern COUNTED_ARTEFACT = Pattern.compile(
            "[0-9]+[ ]+(modules?|files?|classes?|tables?|endpoints?|offenders?|violations?|occurrences?|rules?)");

    private static final String NO_MACHINE_REASON = "No machine:";

    private final List<Rule> rules = RuleCatalogue.rules();

    @Test
    void theCatalogueIsReadableAndNotEmpty() {
        assertThat(rules).isNotEmpty();
    }

    @Test
    void everyCitedExecutorExists() {
        List<String> missing = rules.stream()
                .filter(Rule::declaresAMachine)
                .flatMap(rule -> rule.citedEnforcers().stream().map(enforcer -> rule.id() + " -> " + enforcer))
                .filter(citation -> !enforcerExists(citation.substring(citation.indexOf("-> ") + 3)))
                .toList();

        assertThat(missing)
                .describedAs("a rule claims a machine that does not exist under src/test")
                .isEmpty();
    }

    @Test
    void everyPlannedEnforcerIsStillMissing() {
        List<String> arrived = rules.stream()
                .filter(rule -> rule.machine().equals("planned"))
                .flatMap(rule -> rule.citedEnforcers().stream().map(enforcer -> rule.id() + " -> " + enforcer))
                .filter(citation -> enforcerExists(citation.substring(citation.indexOf("-> ") + 3)))
                .toList();

        assertThat(arrived)
                .describedAs("the enforcer exists, so the row must move from planned to yes")
                .isEmpty();
    }

    @Test
    void ruleIdsAreUniqueAndContiguousWithinTheirGroup() {
        Map<Integer, List<Integer>> numbersByGroup = rules.stream()
                .collect(Collectors.groupingBy(Rule::group,
                        Collectors.mapping(Rule::number, Collectors.toList())));

        numbersByGroup.forEach((group, numbers) -> assertThat(numbers.stream().sorted().toList())
                .describedAs("group R%d", group)
                .containsExactlyElementsOf(numbers.stream().sorted().distinct().toList())
                .isEqualTo(java.util.stream.IntStream.rangeClosed(1, numbers.size()).boxed().toList()));
    }

    @Test
    void everyRuleDeclaresAKnownSeverity() {
        assertThat(rules).allSatisfy(rule -> assertThat(RuleCatalogue.SEVERITIES)
                .describedAs("%s declares severity %s", rule.id(), rule.severity())
                .contains(rule.severity()));
    }

    @Test
    void everyRuleDeclaresAKnownMachineAndStatus() {
        assertThat(rules).allSatisfy(rule -> {
            assertThat(RuleCatalogue.MACHINES)
                    .describedAs("%s declares machine %s", rule.id(), rule.machine())
                    .contains(rule.machine());
            assertThat(RuleCatalogue.STATUSES)
                    .describedAs("%s declares status %s", rule.id(), rule.status())
                    .contains(rule.status());
        });
    }

    @Test
    void theCatalogueDeclaresRulesInsteadOfCountingThings() {
        List<String> counting = rules.stream()
                .filter(rule -> COUNTED_ARTEFACT.matcher(rule.statement()).find())
                .map(Rule::id)
                .toList();

        assertThat(counting)
                .describedAs("the number belongs in the machine, where it cannot go stale in silence")
                .isEmpty();
    }

    @Test
    void everySecurityRuleHasAMachineOrAnOwnedGap() {
        List<String> unguarded = rules.stream()
                .filter(rule -> rule.severity().equals("SEC"))
                .filter(rule -> rule.machine().equals("none"))
                .map(Rule::id)
                .filter(id -> !RuleWaivers.covers(id))
                .toList();

        assertThat(unguarded)
                .describedAs("a security rule with no machine needs a waiver with an owner and a date")
                .isEmpty();
    }

    @Test
    void everyRuleWithoutAMachineSaysWhy() {
        List<String> silent = rules.stream()
                .filter(rule -> rule.machine().equals("none"))
                .filter(rule -> !rule.statement().contains(NO_MACHINE_REASON))
                .map(Rule::id)
                .toList();

        assertThat(silent)
                .describedAs("\"none\" must mean it cannot be built, and the row must say why")
                .isEmpty();
    }

    @Test
    void aRuleWithNoMachineCarriesNoStatus() {
        List<String> inconsistent = rules.stream()
                .filter(rule -> rule.machine().equals("none") && !rule.status().equals("—"))
                .map(Rule::id)
                .toList();

        assertThat(inconsistent).isEmpty();
    }

    private static boolean enforcerExists(String enforcer) {
        String className = enforcer.contains(".") ? enforcer.substring(0, enforcer.indexOf('.')) : enforcer;
        Optional<Path> source = SourceTree.find(className, SourceTree.TEST, SourceTree.INTEGRATION_TEST);
        if (source.isEmpty()) {
            return false;
        }
        if (!enforcer.contains(".")) {
            return true;
        }
        String member = enforcer.substring(enforcer.indexOf('.') + 1);
        return SourceTree.read(source.get()).contains(" " + member + "(");
    }
}
