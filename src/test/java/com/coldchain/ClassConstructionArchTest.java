package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.MappedSuperclass;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ClassConstructionArchTest {

    private static final String ROOT = "com.coldchain";

    private static final String MODULES = ROOT + ".modules.";

    private static final String DOMAIN_MODEL = ".internal.domain.model";

    private static final List<String> FACTORY_NAMES = List.of("createNew", "restore");

    private static final List<String> CONTRACT_SUFFIXES =
            List.of("Command", "Result", "Filter", "Criteria");

    private static final Pattern TOP_LEVEL_TYPE = Pattern.compile(
            "(?m)^(?:public |final |abstract |sealed |non-sealed )*"
                    + "(?:class|interface|record|enum|@interface) ([A-Za-z0-9_]+)");

    private static final Pattern QUALIFIED_NAME_INLINE = Pattern.compile(
            "(?<![A-Za-z0-9_.\"])(?:java|javax|jakarta|org|com)[.][a-z0-9_]+(?:[.][a-z0-9_]+)*[.][A-Z]");

    private static final Path CONTROLLERS = SourceTree.MAIN.resolve(
            Path.of("com", "coldchain", "delivery"));

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void domainAggregatesAreCreatedThroughNamedFactories() {
        List<JavaClass> aggregates = production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .filter(type -> type.getPackageName().endsWith(DOMAIN_MODEL))
                .filter(type -> !type.isEnum() && !type.isRecord() && !type.isInterface())
                .filter(ClassConstructionArchTest::holdsState)
                .toList();

        assertThat(aggregates).isNotEmpty();
        assertThat(aggregates).allSatisfy(aggregate -> {
            assertThat(aggregate.getConstructors())
                    .describedAs("%s can be built without going through a name that says what "
                            + "building it means", aggregate.getFullName())
                    .allSatisfy(constructor -> assertThat(constructor.getModifiers())
                            .doesNotContain(JavaModifier.PUBLIC));
            assertThat(aggregate.getMethods().stream()
                    .filter(method -> method.getModifiers().contains(JavaModifier.STATIC))
                    .filter(method -> method.getRawReturnType().equals(aggregate))
                    .map(method -> method.getName())
                    .toList())
                    .describedAs("%s has no named factory", aggregate.getFullName())
                    .containsAnyElementsOf(FACTORY_NAMES);
        });
    }

    @Test
    void nothingInheritsToShareBehaviour() {
        List<JavaClass> ours = production.stream()
                .filter(type -> !type.isInterface() && !type.isEnum() && !type.isRecord())
                .filter(type -> type.getRawSuperclass().isPresent())
                .toList();

        assertThat(ours).isNotEmpty();
        assertThat(ours).allSatisfy(type -> {
            JavaClass parent = type.getRawSuperclass().orElseThrow();
            assertThat(parent.getName().equals(Object.class.getName())
                    || !parent.getPackageName().startsWith(ROOT)
                    || parent.isAnnotatedWith(MappedSuperclass.class)
                    || parent.isAssignableTo(RuntimeException.class))
                    .describedAs("%s inherits from %s to share behaviour, which composition does "
                            + "without tying the two together", type.getFullName(), parent.getName())
                    .isTrue();
        });
    }

    @Test
    void everyContractTypeIsAnImmutableRecord() {
        List<JavaClass> contracts = production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .filter(type -> CONTRACT_SUFFIXES.stream()
                        .anyMatch(suffix -> type.getSimpleName().endsWith(suffix)))
                .toList();

        assertThat(contracts).isNotEmpty();
        assertThat(contracts).allSatisfy(contract -> assertThat(contract.isRecord())
                .describedAs("%s crosses the boundary as something a caller can still change",
                        contract.getFullName())
                .isTrue());
    }

    @Test
    void everyFileDeclaresOnePublicTypeNamedLikeItself() {
        List<JavaSource.Scan> sources = productionSources();

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(source -> {
            String fileName = source.file().getFileName().toString().replace(".java", "");
            List<String> declared = TOP_LEVEL_TYPE.matcher(source.code()).results()
                    .map(result -> result.group(1))
                    .toList();
            if (fileName.equals("package-info")) {
                return;
            }
            assertThat(declared)
                    .describedAs("%s does not declare exactly one top-level type named after "
                            + "the file", source.file())
                    .containsExactly(fileName);
        });
    }

    @Test
    void noCommentExplainsCodeFromInsideABlock() {
        List<JavaSource.Scan> sources = productionSources();

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(source -> assertThat(source.insideBlocks())
                .describedAs("%s explains itself from inside a block: either the name is wrong or "
                        + "the decision belongs in an ADR", source.file())
                .isEmpty());
    }

    @Test
    void controllersDocumentThemselvesInOpenApi() {
        List<JavaSource.Scan> controllers = JavaSource.scanAll(CONTROLLERS).stream()
                .filter(source -> source.file().getFileName().toString().endsWith("Controller.java"))
                .toList();

        assertThat(controllers).isNotEmpty();
        assertThat(controllers).allSatisfy(controller -> {
            assertThat(controller.comments())
                    .describedAs("%s documents itself in javadoc nobody reading the API will see",
                            controller.file())
                    .isEmpty();
            assertThat(controller.body())
                    .describedAs("%s exposes endpoints it does not describe in the document it "
                            + "generates", controller.file())
                    .contains("@Operation");
        });
    }

    @Test
    void useCasesAndDomainCarryNoJavadoc() {
        List<JavaSource.Scan> sources = productionSources().stream()
                .filter(source -> source.file().toString().contains("usecase")
                        || source.file().toString().contains("domain"))
                .toList();

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(source -> assertThat(source.comments())
                .describedAs("%s carries prose where the code is the statement", source.file())
                .isEmpty());
    }

    @Test
    void importsAreGroupedJavaThenLibrariesThenLocal() {
        List<JavaSource.Scan> sources = productionSources();

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(source -> {
            List<String> statics = source.imports().stream()
                    .filter(name -> name.startsWith("static "))
                    .toList();
            List<String> types = source.imports().stream()
                    .filter(name -> !name.startsWith("static "))
                    .toList();
            assertThat(source.imports())
                    .describedAs("%s mixes the static imports into the rest", source.file())
                    .containsExactlyElementsOf(concat(statics, types));
            assertThat(types)
                    .describedAs("%s does not keep its imports in one sorted block", source.file())
                    .isSorted();
            assertThat(QUALIFIED_NAME_INLINE.matcher(source.body()).results().map(result ->
                    result.group()).toList())
                    .describedAs("%s names a type in full where an import would say it once",
                            source.file())
                    .isEmpty();
        });
    }

    private static List<String> concat(List<String> first, List<String> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }

    private static boolean holdsState(JavaClass type) {
        return type.getFields().stream()
                .anyMatch(field -> !field.getModifiers().contains(JavaModifier.STATIC));
    }

    private List<JavaSource.Scan> productionSources() {
        return JavaSource.scanAll(SourceTree.MAIN);
    }
}
