package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

class EventDeliveryArchTest {

    private static final String ROOT = "com.coldchain";

    private static final List<String> DELIVERY_OUTSIDE_THE_TRANSACTION = List.of(
            "org.springframework.scheduling.annotation.Async",
            "org.springframework.transaction.event.TransactionalEventListener");

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void cascadesRunInsideThePublishingTransaction() {
        List<JavaClass> listeners = production.stream()
                .filter(type -> type.getMethods().stream()
                        .anyMatch(method -> method.isAnnotatedWith(EventListener.class)))
                .toList();
        List<JavaClass> publishers = production.stream()
                .filter(type -> type.getFields().stream()
                        .anyMatch(field -> field.getRawType().isAssignableTo(
                                ApplicationEventPublisher.class)))
                .toList();

        assertThat(listeners).isNotEmpty();
        assertThat(publishers).isNotEmpty();
        ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat(DescribedPredicate.describe(
                        "hand a reaction to another thread or to life after the commit",
                        type -> DELIVERY_OUTSIDE_THE_TRANSACTION.contains(type.getFullName())))
                .because("if the reaction fails the whole operation fails: there is no dispatched "
                        + "shipment with no monitoring window")
                .check(production);
        assertThat(publishers).allSatisfy(publisher -> assertThat(SourceTree.read(
                SourceTree.find(publisher.getSimpleName(), SourceTree.MAIN).orElseThrow()))
                .describedAs("%s publishes an event outside a transaction, so the reaction cannot "
                        + "fail with it", publisher.getFullName())
                .contains("@Transactional"));
    }
}
