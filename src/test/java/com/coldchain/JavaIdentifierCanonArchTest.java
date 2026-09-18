package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class JavaIdentifierCanonArchTest {

    private static final String ROOT = "com.coldchain";

    private static final Pattern LOWERCASE_PACKAGE =
            Pattern.compile("[a-z][a-z0-9]*([.][a-z][a-z0-9]*)*");

    private static final Pattern PASCAL_CASE = Pattern.compile("[A-Z][A-Za-z0-9]*");

    private static final Pattern CAMEL_CASE = Pattern.compile("[a-z][A-Za-z0-9]*");

    private static final Pattern UPPER_SNAKE_CASE = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");

    private static final Pattern ONE_CAPITAL_LETTER = Pattern.compile("[A-Z]");

    private static final List<String> NAMES_THE_LANGUAGE_IMPOSES = List.of("serialVersionUID");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void packagesAreLowercaseAndUnseparated() {
        List<String> packages = production.stream()
                .map(JavaClass::getPackageName)
                .distinct()
                .toList();

        assertThat(packages).isNotEmpty();
        assertThat(packages).allSatisfy(name -> assertThat(LOWERCASE_PACKAGE.matcher(name).matches())
                .describedAs("package %s is not lowercase and unseparated", name)
                .isTrue());
    }

    @Test
    void typesArePascalCase() {
        List<JavaClass> named = declaredTypes();

        assertThat(named).isNotEmpty();
        assertThat(named).allSatisfy(type -> assertThat(PASCAL_CASE.matcher(type.getSimpleName()).matches())
                .describedAs("type %s is not PascalCase", type.getFullName())
                .isTrue());
    }

    @Test
    void membersAreCamelCaseAndConstantsAreUpperSnake() {
        List<JavaMethod> methods = declaredTypes().stream()
                .flatMap(type -> type.getMethods().stream())
                .filter(JavaIdentifierCanonArchTest::isWrittenByHand)
                .toList();
        List<JavaField> fields = declaredTypes().stream()
                .flatMap(type -> type.getFields().stream())
                .filter(JavaIdentifierCanonArchTest::isWrittenByHand)
                .filter(field -> !isEnumConstant(field))
                .filter(field -> !NAMES_THE_LANGUAGE_IMPOSES.contains(field.getName()))
                .toList();

        assertThat(methods).isNotEmpty();
        assertThat(fields).isNotEmpty();
        assertThat(methods).allSatisfy(method -> assertThat(CAMEL_CASE.matcher(method.getName()).matches())
                .describedAs("method %s is not camelCase", method.getFullName())
                .isTrue());
        assertThat(fields).allSatisfy(field -> assertThat(
                nameCanonOf(field).matcher(field.getName()).matches())
                .describedAs("field %s does not follow the canon its modifiers impose", field.getFullName())
                .isTrue());
    }

    @Test
    void enumConstantsAreUpperSnakeCase() {
        List<JavaField> constants = declaredTypes().stream()
                .filter(JavaClass::isEnum)
                .flatMap(type -> type.getFields().stream())
                .filter(JavaIdentifierCanonArchTest::isEnumConstant)
                .toList();

        assertThat(constants).isNotEmpty();
        assertThat(constants).allSatisfy(constant -> assertThat(
                UPPER_SNAKE_CASE.matcher(constant.getName()).matches())
                .describedAs("enum constant %s is not UPPER_SNAKE_CASE", constant.getFullName())
                .isTrue());
    }

    @Test
    void genericParametersAreSingleCapitalLetters() {
        List<String> parameters = declaredTypes().stream()
                .flatMap(type -> Stream.concat(
                        type.getTypeParameters().stream().map(parameter -> parameter.getName()),
                        type.getCodeUnits().stream()
                                .filter(JavaIdentifierCanonArchTest::isWrittenByHand)
                                .flatMap(unit -> unit.getTypeParameters().stream())
                                .map(parameter -> parameter.getName())))
                .toList();

        assertThat(parameters).isNotEmpty();
        assertThat(parameters).allSatisfy(name -> assertThat(ONE_CAPITAL_LETTER.matcher(name).matches())
                .describedAs("type parameter %s is not a single capital letter", name)
                .isTrue());
    }

    private List<JavaClass> declaredTypes() {
        return production.stream()
                .filter(type -> !type.isAnonymousClass())
                .filter(type -> !type.isLocalClass())
                .filter(type -> !type.getSimpleName().isEmpty())
                .filter(type -> !type.getSimpleName().equals("package-info"))
                .toList();
    }

    private static Pattern nameCanonOf(JavaField field) {
        boolean constant = field.getModifiers().contains(JavaModifier.STATIC)
                && field.getModifiers().contains(JavaModifier.FINAL);
        return constant ? UPPER_SNAKE_CASE : CAMEL_CASE;
    }

    private static boolean isEnumConstant(JavaField field) {
        return field.getOwner().isEnum()
                && field.getModifiers().contains(JavaModifier.STATIC)
                && field.getRawType().equals(field.getOwner());
    }

    private static boolean isWrittenByHand(HasModifiers member) {
        return !member.getModifiers().contains(JavaModifier.SYNTHETIC);
    }
}
