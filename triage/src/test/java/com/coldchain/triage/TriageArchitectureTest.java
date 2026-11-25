package com.coldchain.triage;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.coldchain.triage.shared.annotation.UseCase;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.coldchain.triage", importOptions = ImportOption.DoNotIncludeTests.class)
class TriageArchitectureTest {

    @ArchTest
    static final ArchRule theDomainIsPlainJava = noClasses()
            .that().resideInAPackage("..internal.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "org.hibernate..", "tools.jackson..", "org.apache.kafka..")
            .because("the rules of triage are tested in milliseconds, with no framework near them (ADR-007)");

    @ArchTest
    static final ArchRule noUseCaseSeesPersistence = noClasses()
            .that().resideInAPackage("..internal.application..")
            .should().dependOnClassesThat().resideInAPackage("..internal.infrastructure..")
            .because("a use case talks to ports; a JpaEntity in its imports means the design leaked");

    @ArchTest
    static final ArchRule theChannelOnlyKnowsTheDoor = noClasses()
            .that().resideInAPackage("..delivery..")
            .should().dependOnClassesThat().resideInAPackage("..internal..")
            .because("a delivery channel calls TriageApi and knows nothing behind it");

    @ArchTest
    static final ArchRule useCasesLiveInTheirFolder = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .and().areNotAnnotations()
            .should().resideInAnyPackage("..internal.application.usecase.command..",
                    "..internal.application.usecase.query..")
            .andShould().beAnnotatedWith(UseCase.class);

    @ArchTest
    static final ArchRule portsAreInterfacesTheDomainDeclares = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().haveSimpleNameNotEndingWith("JpaRepository")
            .should().resideInAPackage("..internal.domain.repository..")
            .andShould().beInterfaces();

    @ArchTest
    static final ArchRule adaptersLiveInPersistence = classes()
            .that().haveSimpleNameEndingWith("RepositoryAdapter")
            .should().resideInAPackage("..internal.infrastructure.persistence.adapter..");

    @ArchTest
    static final ArchRule jpaEntitiesLiveInPersistence = classes()
            .that().haveSimpleNameEndingWith("JpaEntity")
            .should().resideInAPackage("..internal.infrastructure.persistence.entity..");

    @ArchTest
    static final ArchRule springDataRepositoriesLiveInPersistence = classes()
            .that().haveSimpleNameEndingWith("JpaRepository")
            .should().resideInAPackage("..internal.infrastructure.persistence.jpa..");

    @ArchTest
    static final ArchRule listenersAreDeliveryChannels = classes()
            .that().haveSimpleNameEndingWith("Listener")
            .should().resideInAPackage("..delivery.messaging..");
}
