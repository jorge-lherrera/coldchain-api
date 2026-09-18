package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModuleClosureTest {

    private static final Path PLANS = Path.of("docs");

    private static final Pattern PLAN_FILE = Pattern.compile("plan-[0-9]+-([a-z]+)[.]md");

    private static final Pattern STATUS = Pattern.compile("> [*][*]Status:[*][*] (closed|in progress)");

    private static final Path MODULES = SourceTree.MAIN.resolve(Path.of("com", "coldchain", "modules"));

    private static final Path INTEGRATION = SourceTree.INTEGRATION_TEST.resolve(
            Path.of("com", "coldchain"));

    @Test
    void everyClosedModuleMeetsTheClosureConditions() {
        List<String> closed = plans().stream()
                .filter(plan -> statusOf(plan).equals("closed"))
                .map(ModuleClosureTest::moduleOf)
                .toList();

        assertThat(plans()).isNotEmpty();
        assertThat(closed)
                .describedAs("no module is declared closed, so the bar has never been applied")
                .isNotEmpty();
        assertThat(RuleCatalogue.rules().stream()
                .filter(rule -> rule.status().equals("pending"))
                .map(RuleCatalogue.Rule::id)
                .toList())
                .describedAs("a module is declared closed while the catalogue still owes a machine, "
                        + "which is closure decided by fatigue instead of by the repository")
                .isEmpty();
        assertThat(closed).allSatisfy(module -> {
            assertThat(MODULES.resolve(module).resolve("package-info.java"))
                    .describedAs("module %s is closed without declaring its edges", module)
                    .exists();
            assertThat(MODULES.resolve(module).resolve("api")
                    .resolve(capitalise(module) + "Api.java"))
                    .describedAs("module %s is closed without a door", module)
                    .exists();
            assertThat(SourceTree.javaFiles(INTEGRATION).stream()
                    .map(file -> SourceTree.read(file))
                    .anyMatch(text -> text.contains("modules." + module + ".api")))
                    .describedAs("module %s is closed without a test that exercises it against "
                            + "the database it writes to", module)
                    .isTrue();
        });
    }

    @Test
    void atMostOneModuleIsInProgress() {
        List<String> inProgress = plans().stream()
                .filter(plan -> statusOf(plan).equals("in progress"))
                .map(ModuleClosureTest::moduleOf)
                .toList();

        assertThat(plans()).isNotEmpty();
        assertThat(inProgress)
                .describedAs("more than one module is open at a time, and fixing everything at "
                        + "once is the reason nothing finishes")
                .hasSizeLessThanOrEqualTo(1);
    }

    private static List<Path> plans() {
        try (Stream<Path> files = Files.list(PLANS)) {
            return files
                    .filter(file -> PLAN_FILE.matcher(file.getFileName().toString()).matches())
                    .sorted()
                    .toList();
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not list " + PLANS, cause);
        }
    }

    private static String statusOf(Path plan) {
        Matcher status = STATUS.matcher(SourceTree.read(plan));
        assertThat(status.find())
                .describedAs("%s does not say whether the module it plans is closed or still open",
                        plan)
                .isTrue();
        return status.group(1);
    }

    private static String moduleOf(Path plan) {
        Matcher matcher = PLAN_FILE.matcher(plan.getFileName().toString());
        if (!matcher.matches()) {
            throw new IllegalStateException(plan + " is not a plan");
        }
        return matcher.group(1);
    }

    private static String capitalise(String module) {
        return Character.toUpperCase(module.charAt(0)) + module.substring(1);
    }
}
