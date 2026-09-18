package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SchemaOwnershipArchTest {

    private static final String ROOT = "com.coldchain";

    private static final String MODULES = ROOT + ".modules.";

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void everyTableHasExactlyOneOwningModule() {
        List<String> created = Migrations.tables().stream().map(Migrations.Table::name).sorted().toList();
        Map<String, List<String>> claimantsOf = claimantsOfEachTable(created);

        assertThat(created).isNotEmpty();
        assertThat(mappedTables())
                .describedAs("a mapping points at a table the schema does not create")
                .isSubsetOf(created);
        assertThat(claimantsOf).allSatisfy((table, claimants) -> assertThat(claimants)
                .describedAs("%s belongs to no module, or to more than one, which copies a "
                        + "contract instead of calling it", table)
                .hasSize(1));
    }

    @Test
    void nativeSqlStaysInsideItsOwnModule() {
        Map<String, String> ownerOf = ownerOfEachTable();
        List<Path> sources = SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(file -> moduleOfFile(file) != null)
                .toList();

        assertThat(ownerOf).isNotEmpty();
        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(file -> {
            String module = moduleOfFile(file);
            String text = SourceTree.read(file);
            ownerOf.forEach((table, owner) -> {
                if (owner.equals(module)) {
                    return;
                }
                assertThat(Pattern.compile("\\b" + table + "\\b").matcher(text).find())
                        .describedAs("%s names %s, a table module %s owns: the coupling survives "
                                + "and the declared edge disappears", file, table, owner)
                        .isFalse();
            });
        });
    }

    private Map<String, String> ownerOfEachTable() {
        List<String> created = Migrations.tables().stream().map(Migrations.Table::name).sorted().toList();
        Map<String, String> owners = new TreeMap<>();
        claimantsOfEachTable(created).forEach((table, claimants) -> {
            if (claimants.size() == 1) {
                owners.put(table, claimants.getFirst());
            }
        });
        return owners;
    }

    private Map<String, List<String>> claimantsOfEachTable(List<String> created) {
        Map<String, List<String>> claimants = new TreeMap<>();
        created.forEach(table -> claimants.put(table, new ArrayList<>()));
        production.stream()
                .filter(type -> type.isAnnotatedWith(Entity.class))
                .filter(type -> type.isAnnotatedWith(Table.class))
                .filter(type -> claimants.containsKey(type.getAnnotationOfType(Table.class).name()))
                .forEach(entity -> claimants.get(entity.getAnnotationOfType(Table.class).name())
                        .add(moduleOf(entity.getPackageName())));
        SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(file -> moduleOfFile(file) != null)
                .forEach(file -> {
                    String module = moduleOfFile(file);
                    String text = SourceTree.read(file);
                    claimants.forEach((table, modules) -> {
                        if (!modules.contains(module)
                                && Pattern.compile("\\b" + table + "\\b").matcher(text).find()) {
                            modules.add(module);
                        }
                    });
                });
        return claimants;
    }

    private List<String> mappedTables() {
        return production.stream()
                .filter(type -> type.isAnnotatedWith(Table.class))
                .map(type -> type.getAnnotationOfType(Table.class).name())
                .toList();
    }

    private static String moduleOf(String packageName) {
        String withoutRoot = packageName.substring(MODULES.length());
        return withoutRoot.substring(0, withoutRoot.indexOf('.'));
    }

    private static String moduleOfFile(Path file) {
        List<String> parts = List.of(file.toString().replace('\\', '/').split("/"));
        int index = parts.indexOf("modules");
        return index < 0 || index + 1 >= parts.size() ? null : parts.get(index + 1);
    }
}
