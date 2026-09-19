package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.shared.pagination.PageCriteria;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PersistenceFetchArchTest {

    private static final String ROOT = "com.coldchain";

    private static final String MODULES = ROOT + ".modules.";

    private static final String PORTS = ".internal.domain.repository";

    private static final String APPLICATION = ".internal.application";

    private static final Path CONFIGURATION =
            Path.of("src", "main", "resources", "application.yml");

    private static final Pattern GLOBAL_BATCH_FETCH_SIZE =
            Pattern.compile("default_batch_fetch_size:\\s*([0-9]+)");

    private static final Pattern DECLARED_BATCH_SIZE =
            Pattern.compile("@BatchSize\\s*\\(\\s*size\\s*=\\s*([0-9]+)");

    private static final List<String> READS_OF_A_CLOSED_CATALOGUE = List.of(
            "com.coldchain.modules.identity.internal.domain.repository.RoleRepository.findAll()");

    private static final List<String> IN_MEMORY_AGGREGATIONS = List.of(
            "groupingBy", "counting", "summingInt", "summingLong", "summingDouble",
            "averagingInt", "averagingLong", "averagingDouble");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void theGlobalBatchFetchSizeIsConfigured() {
        Matcher configured = GLOBAL_BATCH_FETCH_SIZE.matcher(SourceTree.read(CONFIGURATION));

        assertThat(configured.find())
                .describedAs("without a global batch fetch size every collection is read one row "
                        + "at a time and nothing in the code says so")
                .isTrue();
        assertThat(Integer.parseInt(configured.group(1))).isPositive();
    }

    @Test
    void noCollectionUnderCutsTheGlobalBatchFetchSize() {
        Matcher configured = GLOBAL_BATCH_FETCH_SIZE.matcher(SourceTree.read(CONFIGURATION));
        assertThat(configured.find()).isTrue();
        int global = Integer.parseInt(configured.group(1));
        List<JavaSource.Scan> sources = JavaSource.scanAll(SourceTree.MAIN);

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(source -> DECLARED_BATCH_SIZE.matcher(source.code()).results()
                .forEach(declared -> assertThat(Integer.parseInt(declared.group(1)))
                        .describedAs("%s asks for fewer rows per round trip than the global size, "
                                + "which costs twice the round trips instead of half", source.file())
                        .isGreaterThanOrEqualTo(global)));
    }

    @Test
    void noUnboundedReadArrivesThroughAnInheritedOverload() {
        List<JavaMethod> reads = production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .filter(type -> type.getPackageName().endsWith(PORTS))
                .flatMap(port -> port.getMethods().stream())
                .filter(method -> method.getRawReturnType().isAssignableTo(Collection.class))
                .toList();

        assertThat(reads).isNotEmpty();
        assertThat(reads).allSatisfy(read -> assertThat(read.getRawParameterTypes().isEmpty()
                && !READS_OF_A_CLOSED_CATALOGUE.contains(read.getFullName()))
                .describedAs("%s reads a table whole: the day the table grows nothing warns",
                        read.getFullName())
                .isFalse());
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(MODULES + "*" + APPLICATION + "..")
                .should().callMethodWhere(DescribedPredicate.describe(
                        "is an unbounded findAll inherited from Spring Data",
                        call -> call.getName().equals("findAll")
                                && call.getTarget().getRawParameterTypes().stream()
                                        .noneMatch(parameter -> parameter.isAssignableTo(
                                                PageCriteria.class))
                                && !READS_OF_A_CLOSED_CATALOGUE.contains(
                                        call.getTarget().getFullName())))
                .because("a listing that forgets its page returns the whole tenant")
                .check(production);
    }

    @Test
    void aggregatesAreProjectedNotComputedInMemory() {
        List<JavaClass> useCases = production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .filter(type -> type.getPackageName().contains(APPLICATION))
                .filter(type -> type.getSimpleName().endsWith("UseCase"))
                .toList();

        assertThat(useCases).isNotEmpty();
        assertThat(useCases).allSatisfy(useCase -> assertThat(useCase.getMethodCallsFromSelf())
                .describedAs("%s counts, groups or adds up in Java what the database was going to "
                        + "be asked anyway", useCase.getFullName())
                .noneMatch(call -> IN_MEMORY_AGGREGATIONS.contains(call.getName())));
    }

    @Test
    void paginationUsesTheAnsiOffsetSyntax() {
        List<Path> written = SourceTree.javaFiles(SourceTree.MAIN);

        assertThat(written).isNotEmpty();
        assertThat(written).allSatisfy(file -> assertThat(SourceTree.read(file).toUpperCase())
                .describedAs("%s pages with ROWNUM, which Oracle applies before the sort and "
                        + "therefore pages over the wrong rows", file)
                .doesNotContain("ROWNUM"));
        assertThat(SourceTree.read(CONFIGURATION))
                .describedAs("the dialect is what turns a page into OFFSET ... FETCH NEXT")
                .contains("dialect: org.hibernate.dialect.OracleDialect");
    }
}
