package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

class ModuleFileLayoutArchTest {

    private static final String ROOT = "com.coldchain";

    private static final String MODULES = ROOT + ".modules.";

    private static final String DELIVERY = ROOT + ".delivery.web.";

    private static final Pattern CHANNEL = Pattern.compile(Pattern.quote(DELIVERY) + "([a-z]+)");

    private static final List<Map.Entry<String, String>> FOLDER_OF_SUFFIX = List.of(
            Map.entry("RepositoryAdapter", "internal.infrastructure.persistence.adapter"),
            Map.entry("PersistenceMapper", "internal.infrastructure.persistence.mapper"),
            Map.entry("JpaRepository", "internal.infrastructure.persistence.jpa"),
            Map.entry("JpaEntity", "internal.infrastructure.persistence.entity"),
            Map.entry("ApiMapper", "internal.application.mapper"),
            Map.entry("ErrorCode", "internal.exception"),
            Map.entry("UseCase", "internal.application.usecase"),
            Map.entry("Repository", "internal.domain.repository"),
            Map.entry("Facade", "internal.application"),
            Map.entry("Criteria", "internal.domain.model"),
            Map.entry("Command", "api.dto"),
            Map.entry("Result", "api.dto"),
            Map.entry("Filter", "api.dto"),
            Map.entry("Api", "api"));

    private static final List<String> SUFFIXES_THAT_MAY_NEST = List.of("UseCase");

    private static final List<String> HTTP_BODY_SUFFIXES = List.of("Request", "Response", "Payload");

    private static final String DOMAIN_MODEL_FOLDER = "internal.domain.model";

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void everySuffixLivesWhereItsRoleSays() {
        List<JavaClass> governed = moduleTypes().stream()
                .filter(type -> roleOf(type).isPresent())
                .toList();

        assertThat(governed).isNotEmpty();
        assertThat(governed).allSatisfy(type -> {
            Map.Entry<String, String> role = roleOf(type).orElseThrow();
            AbstractStringAssert<?> folder = assertThat(folderOf(type))
                    .describedAs("%s carries the %s role and does not live where that role belongs",
                            type.getFullName(), role.getKey());
            if (SUFFIXES_THAT_MAY_NEST.contains(role.getKey())) {
                folder.startsWith(role.getValue());
            } else {
                folder.isEqualTo(role.getValue());
            }
        });
    }

    @Test
    void httpVocabularyStaysInDelivery() {
        List<JavaClass> bodies = production.stream()
                .filter(type -> type.getPackageName().startsWith(DELIVERY))
                .filter(type -> type.getPackageName().endsWith(".dto"))
                .toList();
        List<JavaClass> responses = production.stream()
                .filter(type -> type.getSimpleName().endsWith("Response"))
                .filter(type -> !type.getPackageName().equals(ROOT + ".shared.response"))
                .toList();
        List<JavaClass> contracts = production.stream()
                .filter(type -> type.getPackageName().contains(".api"))
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .toList();

        assertThat(bodies).isNotEmpty();
        assertThat(responses).isNotEmpty();
        assertThat(contracts).isNotEmpty();
        assertThat(bodies).allSatisfy(body -> assertThat(HTTP_BODY_SUFFIXES)
                .describedAs("%s sits among the HTTP bodies without being one", body.getFullName())
                .anySatisfy(suffix -> assertThat(body.getSimpleName()).endsWith(suffix)));
        assertThat(responses).allSatisfy(response -> assertThat(response.getPackageName())
                .describedAs("%s speaks HTTP outside the channel that speaks HTTP and outside the "
                        + "envelope every channel shares", response.getFullName())
                .startsWith(DELIVERY));
        assertThat(contracts).allSatisfy(contract -> assertThat(contract.getSimpleName())
                .describedAs("%s makes the module contract depend on the shape of a request",
                        contract.getFullName())
                .doesNotEndWith("Request"));
    }

    @Test
    void everyChannelMapperSitsInItsChannel() {
        List<JavaClass> mappers = production.stream()
                .filter(type -> type.getSimpleName().endsWith("WebMapper"))
                .toList();
        List<String> channelsWithBodies = production.stream()
                .filter(type -> type.getPackageName().startsWith(DELIVERY))
                .filter(type -> type.getPackageName().endsWith(".dto"))
                .map(type -> channelOf(type.getPackageName()))
                .distinct()
                .toList();

        assertThat(mappers).isNotEmpty();
        assertThat(channelsWithBodies).isNotEmpty();
        assertThat(mappers).allSatisfy(mapper -> assertThat(mapper.getPackageName())
                .describedAs("%s translates for a channel it does not live in", mapper.getFullName())
                .isEqualTo(DELIVERY + channelOf(mapper.getPackageName()) + ".mapper"));
        assertThat(channelsWithBodies).allSatisfy(channel -> assertThat(mappers.stream()
                .filter(mapper -> mapper.getPackageName().equals(DELIVERY + channel + ".mapper"))
                .count())
                .describedAs("channel %s translates its bodies somewhere other than its one mapper",
                        channel)
                .isEqualTo(1));
    }

    @Test
    void onlyDomainModelsLiveInTheDomainModelFolder() {
        List<JavaClass> models = moduleTypes().stream()
                .filter(type -> folderOf(type).equals(DOMAIN_MODEL_FOLDER))
                .toList();

        assertThat(models).isNotEmpty();
        assertThat(models).allSatisfy(model -> roleOf(model)
                .ifPresent(role -> assertThat(role.getValue())
                        .describedAs("%s wears the %s role inside the domain model folder",
                                model.getFullName(), role.getKey())
                        .isEqualTo(DOMAIN_MODEL_FOLDER)));
    }

    private List<JavaClass> moduleTypes() {
        return production.stream()
                .filter(type -> type.getPackageName().startsWith(MODULES))
                .filter(type -> !type.getSimpleName().equals("package-info"))
                .filter(type -> !type.isAnonymousClass())
                .toList();
    }

    private static Optional<Map.Entry<String, String>> roleOf(JavaClass type) {
        return FOLDER_OF_SUFFIX.stream()
                .filter(role -> type.getSimpleName().endsWith(role.getKey()))
                .findFirst();
    }

    private static String folderOf(JavaClass type) {
        String withoutRoot = type.getPackageName().substring(MODULES.length());
        return withoutRoot.substring(withoutRoot.indexOf('.') + 1);
    }

    private static String channelOf(String packageName) {
        Matcher matcher = CHANNEL.matcher(packageName);
        if (!matcher.find()) {
            throw new IllegalStateException(packageName + " is not a delivery channel");
        }
        return matcher.group(1);
    }
}
