package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ContractVersionArchTest {

    private static final String ROOT = "com.coldchain";

    private static final String MODULES = ROOT + ".modules.";

    private static final Path CONTRACT_SNAPSHOT = Path.of("rules", "contract-v1.txt");

    private static final Path ERROR_CODE_SNAPSHOT = Path.of("rules", "error-codes-v1.txt");

    private static final String WRITE_SNAPSHOT = "contract.snapshot";

    private static final Pattern PUBLISHED_CODE =
            Pattern.compile("([A-Z][A-Z0-9_]*)[(]\\s*\"([a-z][a-z0-9_.]*)\"");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void theContractOnlyGrowsWithinAVersion() {
        List<String> current = currentContract();

        assertThat(current).isNotEmpty();
        assertThat(current)
                .describedAs("something the published contract promised is gone, renamed or of "
                        + "another type: within a version the contract only grows")
                .containsAll(snapshot(CONTRACT_SNAPSHOT, current));
    }

    @Test
    void everyDeprecatedEndpointAnnouncesItsSunset() {
        List<JavaSource.Scan> controllers = JavaSource.scanAll(SourceTree.MAIN).stream()
                .filter(source -> source.file().getFileName().toString().endsWith("Controller.java"))
                .toList();

        assertThat(controllers).isNotEmpty();
        assertThat(controllers).allSatisfy(controller -> {
            if (!controller.code().contains("@Deprecated")) {
                return;
            }
            assertThat(controller.code())
                    .describedAs("%s withdraws an endpoint without announcing when it goes and "
                            + "without saying so in the response", controller.file())
                    .contains("Deprecation")
                    .contains("Sunset");
        });
    }

    @Test
    void noPublishedErrorCodeChangesItsMeaning() {
        List<String> current = currentErrorCodes();

        assertThat(current).isNotEmpty();
        assertThat(current)
                .describedAs("a published error code was recycled, and the client branches on "
                        + "that value")
                .containsAll(snapshot(ERROR_CODE_SNAPSHOT, current));
    }

    private List<String> currentContract() {
        return production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .filter(type -> type.getPackageName().contains(".api"))
                .filter(type -> type.getModifiers().contains(JavaModifier.PUBLIC))
                .flatMap(ContractVersionArchTest::surfaceOf)
                .sorted()
                .distinct()
                .toList();
    }

    private static List<String> currentErrorCodes() {
        return SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(file -> file.getFileName().toString().endsWith("ErrorCode.java"))
                .flatMap(file -> {
                    String owner = file.getFileName().toString().replace(".java", "");
                    return PUBLISHED_CODE.matcher(SourceTree.read(file)).results()
                            .map(result -> owner + "." + result.group(1) + "=" + result.group(2));
                })
                .sorted()
                .toList();
    }

    private static Stream<String> surfaceOf(JavaClass type) {
        Stream<String> members = type.getMethods().stream()
                .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                .filter(method -> !method.getName().equals("toString"))
                .filter(method -> !method.getName().equals("hashCode"))
                .filter(method -> !method.getName().equals("equals"))
                .map(ContractVersionArchTest::signatureOf);
        return Stream.concat(Stream.of(type.getFullName()), members);
    }

    private static String signatureOf(JavaMethod method) {
        return method.getOwner().getFullName() + "#" + method.getName() + "("
                + String.join(",", method.getRawParameterTypes().stream()
                        .map(JavaClass::getName)
                        .toList())
                + "):" + method.getRawReturnType().getName();
    }

    private static List<String> snapshot(Path file, List<String> current) {
        if (System.getProperty(WRITE_SNAPSHOT, "").equals("write")) {
            write(file, current);
        }
        assertThat(file)
                .describedAs("without a recorded contract there is nothing to compare today's "
                        + "against, so run the gate once with -D" + WRITE_SNAPSHOT + "=write")
                .exists();
        try {
            return Files.readAllLines(file).stream().filter(line -> !line.isBlank()).toList();
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not read " + file, cause);
        }
    }

    private static void write(Path file, List<String> current) {
        try {
            Files.write(file, current);
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not write " + file, cause);
        }
    }
}
