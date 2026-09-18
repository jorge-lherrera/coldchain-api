# Project rules — the single catalogue

> **What this document is.** The complete statement of what this project enforces: from the folder down
> to the field, from the database to the shape of an HTTP response. A rule of this project that is not
> here is not a rule of this project.
>
> **What it is not.** It is not a guide and it does not argue. Each rule is one checkable sentence, its
> severity, and who checks it. The *why* lives in the ADR or in the long document the row links to.
>
> **This document verifies itself.** `RuleCatalogConsistencyTest` parses it and fails if a rule cites an
> enforcer that does not exist, declares a machine without naming it, or if an id repeats or leaves a
> gap. It is the one rule that protects all the others.
>
> How to read a row, the severity scale and the `Machine` / `Status` axes are in
> [`README.md`](README.md). Created 2026-09-16.

---

## R0 — Rules about the rules

Governs the rest. Without this group the catalogue decays at the rate new rules are written.

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R0.1 | **This document is the authority.** It is the only place a rule is declared. The others — ADRs, [`database-rules.md`](database-rules.md), `CONTRIBUTING.md`, `CLAUDE.md` — explain and illustrate but do not declare: they cite the id. If a document and the catalogue disagree, the catalogue wins and the other is corrected in the same change. **No machine:** no test can decide whether a paragraph in an ADR *declares* a rule or merely cites it | INT | — | none | — |
| R0.2 | **A rule that declares a machine names it, and the named one exists.** `Machine = yes` or `partial` requires citing at least one enforcer, and that enforcer must exist as a class and as a member under `src/test` | INT | `RuleCatalogConsistencyTest.everyCitedExecutorExists` | yes | green |
| R0.3 | **A `planned` machine is one that does not exist yet, and the catalogue knows it.** The day the class appears, the row must move to `yes` or the build fails. This is the state that stops "we will write the test later" from quietly becoming "nobody ever wrote it" | INT | `RuleCatalogConsistencyTest.everyPlannedEnforcerIsStillMissing` | yes | green |
| R0.4 | **One concept, one representation.** No rule is stated twice. If it has to appear in two documents, one cites the id and does not repeat the statement. **No machine:** detecting that two texts state the same rule requires understanding them, not comparing them | STYLE | — | none | — |
| R0.5 | **`BUILD SUCCESSFUL` does not prove a test ran.** A test cannot demonstrate that it executed itself: commenting it out, annotating it `@Disabled` or renaming it makes it vanish with nothing failing, and the build goes green **because** the check stopped existing. So the check lives in the build, not in the suite: `rulesRan` reads the results XML afterwards and requires every cited enforcer to appear as an executed, non-skipped case | INT | `rulesRan` (Gradle task) · `rulesDbRan` (Gradle task) · `RuleGateCoverageTest.somethingChecksThatTheRulesActuallyRan` | yes | green |
| R0.6 | **Ids are unique and run consecutively within their group**, with no gaps | STYLE | `RuleCatalogConsistencyTest.ruleIdsAreUniqueAndContiguousWithinTheirGroup` | yes | green |
| R0.7 | **Every tolerated red and every green that proves nothing is in the waiver register, with an owner and an expiry date.** Without both it is not tolerance: it is a violation nobody is watching. Green-by-absence belongs here by right — it has detected nothing while appearing to do the opposite, which is worse than a red | INT | `RuleWaiverTest.everyWaiverNamesAnOwnerAndAnExpiryDate` · `RuleWaiverTest.theRegisterIsReadFromTheCatalogueColumns` | yes | green |
| R0.8 | **Every rule declares a severity**, and the backlog is ordered by severity before group | INT | `RuleCatalogConsistencyTest.everyRuleDeclaresAKnownSeverity` | yes | green |
| R0.9 | **The catalogue declares rules, it does not count things.** No row carries a measured figure from the code — how many modules, how many files, how many offenders. **The number lives in the machine**, which is where it cannot go stale in silence: if it changes, the test fails. The machine is partial: it catches a count written in digits, not one spelled out | INT | `RuleCatalogConsistencyTest.theCatalogueDeclaresRulesInsteadOfCountingThings` | partial | green |
| R0.10 | **Every rule with a machine runs in `./gradlew rules`.** A rule that only executes when somebody runs the whole suite gets checked once a month. The gate selects by location, not by list — every enforcement class lives directly in `com.coldchain` — so a new rule joins on its own and none is left out by oversight | INT | `RuleGateCoverageTest.everyClassThatDeclaresRulesLivesWhereTheGateLooks` | yes | green |
| R0.11 | **The gate runs on every push and every pull request**, and the workflow cannot carry `-x test` or `--continue`, which are the two known ways of leaving a build green without having checked anything | SEC | `RuleGateCoverageTest.continuousIntegrationRunsTheGateAndCannotSkipIt` | yes | green |
| R0.12 | **A waiver names the violations it tolerates one by one, and the ratchet fails in both directions.** A new one that is not on the list: red — debt does not grow. One that disappears from the list: red too, until it is removed — ground gained is not given back. A numeric quota does not work, because it lets a new violation hide behind a fixed one | INT | `RuleWaiverTest.aViolationThatIsNotOnTheListFailsBecauseDebtDoesNotGrow` · `RuleWaiverTest.aViolationThatDisappearedAlsoFailsBecauseGroundGainedIsNotGivenBack` | yes | green |
| R0.13 | **A security rule is born with a machine, or with a signed gap.** Writing "the tenant filter runs on every query" costs one line and reads like protection; building what proves it costs a day. Without this rule the cheap half is always available, and a catalogue full of unverified security claims is worse than none: people stop checking what they believe already checked | SEC | `RuleCatalogConsistencyTest.everySecurityRuleHasAMachineOrAnOwnedGap` | yes | green |
| R0.14 | **A rule with no machine says why it has none.** `Machine = none` must mean *it cannot be built*, never *nobody built it yet* — that second one is `planned`. Writing the reason is cheap; what it prevents is a reader assuming a rule is checked because most of them are | INT | `RuleCatalogConsistencyTest.everyRuleWithoutAMachineSaysWhy` | yes | green |
| R0.15 | **The gate has no switch.** No environment variable that skips it, no flag that lets it through. A documented shortcut gets used: the day somebody is in a hurry they use it once, and from then on always. If a rule gets in the way it is fixed or given a waiver with a name and a date; what does not exist is a way to step over it without leaving a trace | SEC | `RuleGateCoverageTest.theGateHasNoBackDoor` | yes | green |
| R0.16 | **No rule passes on an empty selector.** `allowEmptyShould(true)` turns "I found nothing to look at" into green, and the day the classes appear nothing warns that the rule started mattering. Without the concession, ArchUnit *is* the tripwire — and one that cannot disappear, because it is not a separate file | INT | `RuleGateCoverageTest.noRuleIsAllowedToPassOnAnEmptySelector` | yes | green |
| R0.17 | **A rule that needs infrastructure runs in `rulesDb`, and is declared as such.** The gate separates by suffix: `*IT` goes to the slow one and the rest to the fast one. Without this, renaming a class to `*IT` would drop it from the fast gate with nobody checking it reached the slow one | INT | `RuleGateCoverageTest.everyRuleThatNeedsOracleIsDeclared` | yes | green |

---

## R1 — The module: what it is and what it may do

A module is a **bounded context**: a package under `modules/` with a public contract and everything
else hidden. See [ADR-006](../docs/adr/ADR-006-modules-and-events.md).

### R1.a — Shape

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R1.1 | A module lives in `com.coldchain.modules.<name>` and its name is **one lowercase word with no separators**. The five are `identity`, `catalog`, `shipment`, `telemetry`, `compliance` | STYLE | `ModuleShapeArchTest.everyModuleIsNamedInOneLowercaseWord` | yes | green |
| R1.2 | A module has exactly **two top-level folders**: `api/` and `internal/`. There is no third | STYLE | `ModuleShapeArchTest.everyModuleHasExactlyApiAndInternal` | yes | green |
| R1.3 | Under `internal/` the folders are `domain/`, `application/`, `infrastructure/` and `exception/`. No others | STYLE | `ModuleShapeArchTest.everyInternalHoldsOnlyItsFourFolders` | yes | green |
| R1.4 | The layout is fixed where it decides the design: `domain/{model,repository,service}`, `application/{usecase,mapper}` and `infrastructure/persistence/{entity,jpa,adapter,mapper}` ([ADR-007](../docs/adr/ADR-007-hexagonal-module-internals.md)). Under `infrastructure/` a module also holds one folder per outbound technology it actually speaks — `security/` for token issuing and hashing, later a `messaging/` or an `external/`. What is fixed is the shape of persistence, not how many things the outside world is made of | STYLE | `ModuleShapeArchTest.everyLayerKeepsItsFixedLayout` | yes | green |
| R1.5 | A module declares its identity and its allowed dependencies in `package-info.java` with `@ApplicationModule` | STYLE | `ModuleEdgeDeclarationArchTest.everyRealEdgeIsADeclaredEdge` | yes | green |
| R1.6 | **Declared** and **real** dependencies match in both directions: no undeclared edge, and no declaration without an edge | INT | `ModuleEdgeDeclarationArchTest.everyRealEdgeIsADeclaredEdge` | yes | green |
| R1.7 | No module forms a cycle with another | INT | `ArchitectureRulesArchTest.modulesAreFreeOfCycles` | yes | green |
| R1.8 | **The module model builds.** Spring Modulith raises it from the `package-info` files and fails when a module declares a dependency on something that is not a module. Without this, R1.5 and R1.6 can be green over a model that no longer builds | INT | `ModulithVerificationTest.theModuleModelBuilds` | yes | green |

### R1.b — What it may and may not do

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R1.9 | **No module reaches into another's `internal/`.** No exception, `delivery` included | INT | `ModuleBoundariesArchTest.moduleInternalsAreOnlyAccessedWithinTheirModule` | yes | green |
| R1.10 | **Every cross-module access goes through `api/`.** The `<X>Api` interface is the only door | INT | `ArchitectureRulesArchTest.apiPackagesNeverDependOnInternals` | yes | green |
| R1.11 | **`api/` does not depend on `internal/`.** The contract does not drag the implementation behind it | INT | `ArchitectureRulesArchTest.apiPackagesNeverDependOnInternals` | yes | green |
| R1.12 | **Every table has exactly one owning module.** A second mapping from another module is a contract copied instead of called: the coupling survives, the edge disappears, and the column names become an agreement nobody signed | INT | `SchemaOwnershipArchTest.everyTableHasExactlyOneOwningModule` | yes | green |
| R1.13 | **A query does not name tables outside its module.** Not native SQL, not JPQL, not `JdbcTemplate` | INT | `SchemaOwnershipArchTest.nativeSqlStaysInsideItsOwnModule` | yes | green |
| R1.14 | **A module does not write to another module's tables** — not even when seeding, which is one of the reasons reference data ships in migrations ([ADR-008](../docs/adr/ADR-008-reference-data-in-flyway.md)) | INT | `SchemaOwnershipArchTest.nativeSqlStaysInsideItsOwnModule` | yes | green |

### R1.c — The two packages that are not modules

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R1.15 | **`shared/` holds no business logic.** Value types, domain-free utilities and cross-cutting contracts only | STYLE | `ModuleShapeArchTest.sharedHoldsNoBusiness` | yes | green |
| R1.16 | **`delivery/` is an adapter, not part of the module.** `delivery.web.shipment` does not belong to module `shipment`, and R1.9 applies to it just the same | INT | `ModuleBoundariesArchTest.moduleInternalsAreOnlyAccessedWithinTheirModule` | yes | green |
| R1.17 | A type moves up into `shared/` only when **more than one module** consumes it. **No machine:** "more than one module consumes it" is measurable, but "it should move up" is a judgement about the future | STYLE | — | none | — |

---

## R2 — The file: where each thing lives and what it is called

The suffix **is** the contract: it says what role the type plays and therefore which folder it may
live in.

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R2.1 | `<X>Api` — inbound port, in `modules/<x>/api/` | STYLE | `ArchitectureRulesArchTest.apiInterfacesAreNamedApi` | yes | green |
| R2.2 | `<X>Facade` — implementation of the port, in `modules/<x>/internal/application/` | STYLE | `ModuleFileLayoutArchTest.everySuffixLivesWhereItsRoleSays` | yes | green |
| R2.3 | `<X>UseCase` — one use case, in `internal/application/usecase/{command,query}/` | STYLE | `ArchitectureRulesArchTest.useCaseClassesAreNamedUseCase` | yes | green |
| R2.4 | `<X>Command` — an intent coming in, in `api/dto/` | STYLE | `ModuleFileLayoutArchTest.everySuffixLivesWhereItsRoleSays` | yes | green |
| R2.5 | `<X>Result` — what a use case returns, in `api/dto/` | STYLE | `ModuleFileLayoutArchTest.everySuffixLivesWhereItsRoleSays` | yes | green |
| R2.6 | `<X>Filter` — read criteria of the **contract**, in `api/dto/` | STYLE | `ModuleFileLayoutArchTest.everySuffixLivesWhereItsRoleSays` | yes | green |
| R2.7 | `<X>Criteria` — read criteria of the **domain**, in `internal/domain/model/` | STYLE | `ModuleFileLayoutArchTest.everySuffixLivesWhereItsRoleSays` | yes | green |
| R2.8 | An event a module publishes is a `record` in `api/event/` permitted by that module's sealed `<Module>Event` interface, and it is named as the fact it states (`ShipmentDispatched`), not with an `Event` suffix. Nothing outside `api/event/` implements the interface | STYLE | `ModuleFileLayoutArchTest.everySuffixLivesWhereItsRoleSays` | yes | green |
| R2.9 | `<X>Request` / `<X>Response` — HTTP and only HTTP, in `delivery/web/<x>/dto/`, together with the `<X>Payload` pieces they nest. Nothing outside the channel is a `Response`, and the module contract never names a `Request` | STYLE | `ModuleFileLayoutArchTest.httpVocabularyStaysInDelivery` | yes | green |
| R2.10 | `<X>WebMapper` — Request↔Command, Result↔Response, in the delivery channel | STYLE | `ModuleFileLayoutArchTest.everyChannelMapperSitsInItsChannel` | yes | green |
| R2.11 | `<X>ApiMapper` — domain model ↔ `api/` DTO | STYLE | `ModuleFileLayoutArchTest.everySuffixLivesWhereItsRoleSays` | yes | green |
| R2.12 | `<X>JpaEntity` — persistence entity | STYLE | `NamingStandardsArchTest.jpaEntitiesUseJpaEntitySuffix` | yes | green |
| R2.13 | `<X>JpaRepository` — Spring Data repository, in `internal/infrastructure/persistence/jpa/` | STYLE | `ModuleFileLayoutArchTest.everySuffixLivesWhereItsRoleSays` | yes | green |
| R2.14 | `<X>Repository` — **the port**, always an interface, in `internal/domain/repository/` | STYLE | `ArchitectureRulesArchTest.repositoryPortsAreInterfaces` | yes | green |
| R2.15 | `<X>RepositoryAdapter` — implementation of the port | STYLE | `ModuleFileLayoutArchTest.everySuffixLivesWhereItsRoleSays` | yes | green |
| R2.16 | `<X>ErrorCode` — the module's error catalogue, in `internal/exception/` | STYLE | `ModuleFileLayoutArchTest.everySuffixLivesWhereItsRoleSays` | yes | green |
| R2.17 | Domain model — **no suffix**, in `internal/domain/model/` | STYLE | `ModuleFileLayoutArchTest.onlyDomainModelsLiveInTheDomainModelFolder` | yes | green |

### R2.b — The `api/` contract

A blacklist only catches the mistakes already made: the eighth bad suffix is legal until somebody
notices. The canon is the other way round — a whitelist.

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R2.18 | **`api/` admits exactly seven things**: the `<Module>Api` door and the contract enums at its root; `Command`, `Filter`, `Result` and the value records they carry in `dto/`; and the published facts in `event/`. Nothing else | STYLE | `ApiContractCanonArchTest.apiPackagesCarryOnlyTheSevenAllowedSuffixes` | yes | green |
| R2.19 | **Every read DTO converges on `Result`.** No `Summary`, `View`, `Row`, `Detail`, `Info` or `Data`: the nuance goes in the **prefix**, never in the suffix, because at the point of use four suffixes for one role hide which of them may cross the boundary | STYLE | `ApiContractCanonArchTest.everyReadDtoConvergesOnResult` | yes | green |
| R2.20 | **Forbidden suffixes in `api/`**: `Dto` (does not say whether it comes in or goes out), `Request`/`Response` (HTTP vocabulary, and `api/` does not know HTTP exists), `Mapper` (implementation, not contract), `Service` (not a role in this project), `Query` (served two concepts that were indistinguishable at the point of use — use `Filter` or `Criteria`), `Ref` (an abbreviation covering four roles) | STYLE | `ArchitectureRulesArchTest.apiPackagesHaveNoForbiddenSuffixes` | yes | green |

### R2.c — Java identifiers

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R2.21 | **Packages lowercase, no separators** (JLS §6.1). No underscores, no camelCase | STYLE | `JavaIdentifierCanonArchTest.packagesAreLowercaseAndUnseparated` | yes | green |
| R2.22 | **Classes, interfaces, records and enums in `PascalCase`** | STYLE | `JavaIdentifierCanonArchTest.typesArePascalCase` | yes | green |
| R2.23 | **Methods, variables and fields in `camelCase`; `static final` constants in `UPPER_SNAKE_CASE`** | STYLE | `JavaIdentifierCanonArchTest.membersAreCamelCaseAndConstantsAreUpperSnake` | yes | green |
| R2.24 | **Enum constants in `UPPER_SNAKE_CASE`**, the type in `PascalCase` (`Scope.SHIPMENT_WRITE`) | STYLE | `JavaIdentifierCanonArchTest.enumConstantsAreUpperSnakeCase` | yes | green |
| R2.25 | **Generic type parameters, one capital letter** (`<T>`, `<K, V>`) | STYLE | `JavaIdentifierCanonArchTest.genericParametersAreSingleCapitalLetters` | yes | green |
| R2.26 | **All code in English**: names, types, log messages, commit subjects. **No machine:** telling an English identifier from a Spanish one needs a dictionary, and the false positives would kill the rule | STYLE | — | none | — |

---

## R3 — The class: how it is built

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R3.1 | **Constructor injection, always.** `@Autowired`, `@Inject` and `@Resource` on a field or a setter are forbidden | STYLE | `ArchitectureRulesArchTest.noFieldInjection` · `ArchitectureRulesArchTest.noSetterInjection` | yes | green |
| R3.2 | **The domain does not know the framework**: no Spring, no JPA, no Hibernate, no Jackson in `internal/domain/` | STYLE | `ArchitectureRulesArchTest.domainModelsAreFrameworkFree` | yes | green |
| R3.3 | **An aggregate is created through a named factory** (`Shipment.createNew(...)`), never through a public no-args constructor. A no-args constructor exists on the JPA entity, which is a different class | INT | `ClassConstructionArchTest.domainAggregatesAreCreatedThroughNamedFactories` | yes | green |
| R3.4 | **Composition over inheritance.** No class of this project inherits from another to share behaviour. The only parents allowed are `@MappedSuperclass`, `RuntimeException` and the base class a framework requires in order to plug in | STYLE | `ClassConstructionArchTest.nothingInheritsToShareBehaviour` | yes | green |
| R3.5 | **Every JPA entity extends `AuditableEntity`** and therefore carries the four audit columns (R7.12) | INT | `EntityCanonArchTest.everyEntityIsAuditable` | yes | green |
| R3.6 | **Persistence declares no transactions**, neither on the class nor on the method (R11.1) | INT | `ArchitectureRulesArchTest.persistenceDeclaresNoTransactions` | yes | green |
| R3.7 | An entity that names a column outside its field mappings — in an `@SQLRestriction` or in the `columnList` of an `@Index` — **maps that column**. Otherwise every `SELECT` dies with `ORA-00904`, silently | INT | `EntityCanonArchTest.everyRestrictedColumnIsMapped` | yes | green |
| R3.8 | `Command`, `Result`, `Filter` and `Criteria` are **immutable `record`s** | STYLE | `ClassConstructionArchTest.everyContractTypeIsAnImmutableRecord` | yes | green |
| R3.9 | A file declares **one public type** and is named after it | STYLE | `ClassConstructionArchTest.everyFileDeclaresOnePublicTypeNamedLikeItself` | yes | green |
| R3.10 | **A use case is annotated `@UseCase`, never `@Service`.** The stereotype says what the class is, and `@Service` does not distinguish a use case from anything else | STYLE | `ApiContractCanonArchTest.useCasesAreAnnotatedAsUseCasesNotAsServices` | yes | green |
| R3.11 | **No comment inside a code block.** If a block needs explaining, the name is wrong or the decision belongs in an ADR | STYLE | `ClassConstructionArchTest.noCommentExplainsCodeFromInsideABlock` | yes | green |
| R3.12 | **Controllers document themselves in OpenAPI annotations, never in javadoc**; use cases and domain carry no javadoc at all; infrastructure and configuration carry javadoc only when it explains a *why* | STYLE | `ClassConstructionArchTest.controllersDocumentThemselvesInOpenApi` · `ClassConstructionArchTest.useCasesAndDomainCarryNoJavadoc` | yes | green |
| R3.13 | **Imports at the top, the static ones first and every other one in a single ASCII-sorted block**, with no qualified name written inline. One order decided once, so a diff shows what changed instead of who formatted it | STYLE | `ClassConstructionArchTest.importsAreGroupedJavaThenLibrariesThenLocal` | yes | green |

---

## R4 — The field: type, name, annotation

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R4.1 | **A boolean carries no verb prefix.** `active`, never `isActive`. The JSON comes out `active` | CON | `NamingStandardsArchTest.booleanFieldsHaveNoVerbPrefix` | yes | green |
| R4.2 | **An instant is `Instant`.** Never `Date`, never `LocalDateTime` (N4.3) | INT | `NamingStandardsArchTest.timestampsAvoidLegacyDateTypes` | yes | green |
| R4.3 | **A key is a `UUID`** in Java and `RAW(16)` in Oracle, with no `length` and no `@JdbcTypeCode`, and it is a **UUID v7** generated in the application (N1.2, [ADR-002](../docs/adr/ADR-002-uuid-v7-raw16.md)) | INT | `SchemaStandardIT.n1_2_uuidKeysAreStoredAsRaw16` · `UuidRawBindingIT.theSixteenBytesAreTheSameOnesHibernateWrites` | yes | green |
| R4.4 | **Column names in `UPPER_SNAKE_CASE`** in `@Column` and `@JoinColumn`; **table names in `UPPER_SNAKE_CASE` and singular** in `@Table` | STYLE | `NamingStandardsArchTest.persistenceNamesAreUpperSnakeCase` · `PersistenceNamingCanonArchTest.tableNamesAreSingular` | yes | green |
| R4.5 | **A reference to another table is `<TARGET_TABLE>_ID`** (`SHIPMENT_ID`, not `SHIPMENT` nor `ID_SHIPMENT`), checked on every declared foreign key. A `<WHO>_BY` column names a person on purpose and is the one exception; an identifier from another context has no foreign key to check and is R4.10's business | STYLE | `EntityCanonArchTest.everyReferenceColumnIsNamedAfterItsTarget` | yes | green |
| R4.6 | **Every declared index and constraint carries a canonical name**: `PK_<TABLE>`, `FK_<CHILD>_<PARENT>`, `UQ_<TABLE>_<COLUMNS>`, `CK_<TABLE>_<WHAT>` for constraints, and `IX_<TABLE>_<COLUMNS>` / `UX_<TABLE>_<COLUMNS>` for plain and unique indexes. A `SYS_C0015138` does not say what it guarantees, so an integrity failure in production is not diagnosed, it is investigated (N9.1) | STYLE | `PersistenceNamingCanonArchTest.everyDeclaredConstraintCarriesACanonicalName` | yes | green |
| R4.7 | **The same concept is named the same across the whole schema.** The machine is partial and always will be: it catches the synonym somebody already wrote and banned, never the one invented tomorrow | STYLE | `NamingSynonymArchTest.fieldsAndColumnsDoNotUseBannedSynonyms` | partial | green |
| R4.8 | **A table read by tenant declares that index on the entity**, not only in the DDL (N3.2). A table that carries the tenancy column for the check but is never read by it — the custody log, the audit trail — is not indexed by it, because an index nobody reads is a write nobody asked for | COST | `SchemaDeclarationArchTest.tenantColumnIndexIsDeclaredOnTheEntity` | yes | green |
| R4.9 | **A quantity that is summed or compared is `NUMBER(p,s)`.** Never `BINARY_DOUBLE`, never text. A temperature is `NUMBER(5,2)` and a duration is whole seconds (N4.1) | INT | `SchemaStandardIT.n4_1_measurableQuantitiesAreExactDecimals` | yes | green |
| R4.10 | **An identifier from another bounded context is a `UUID` column with no foreign key**, never an embedded object ([ADR-006](../docs/adr/ADR-006-modules-and-events.md)). The single deliberate exception is the tenancy column, R10.6 | INT | `EntityCanonArchTest.noEntityHoldsAnotherModulesEntity` | yes | green |
| R4.11 | **No collection undercuts the global `default_batch_fetch_size`** with its own `@BatchSize`: set below the global, it costs twice the round trips | COST | `PersistenceFetchArchTest.noCollectionUnderCutsTheGlobalBatchFetchSize` | yes | green |
| R4.12 | **A frozen snapshot column is named for what it is and never written twice.** No entity offers a setter and no domain model holds a field that is not `final`, so the five threshold columns on `SHIPMENT` are set once at dispatch and a second write would have to build a new object ([ADR-004](../docs/adr/ADR-004-frozen-thresholds.md)) | INT | `EntityCanonArchTest.frozenColumnsHaveNoSetter` | yes | green |

---

## R5 — The method and the signature

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R5.1 | **A use case does one thing**, has one public `execute`, and is named after what it does | STYLE | `ArchitectureRulesArchTest.useCaseClassesAreNamedUseCase` | yes | green |
| R5.2 | **Command and query separated**: `usecase/command/` writes, `usecase/query/` reads and never writes | STYLE | `LayerContractArchTest.queriesDoNotWrite` | yes | green |
| R5.3 | **No paginated endpoint without declared sortable fields.** An open `sort` parameter is an open door to ordering by an unindexed column | CON | `ArchitectureRulesArchTest.pageableEndpointsDeclareSortableFields` | yes | green |
| R5.4 | **No unbounded read.** `findAll()` and `findAll(Specification)` inherited from Spring Data do not reach a use case; every listing is `Pageable` | COST | `PersistenceFetchArchTest.noUnboundedReadArrivesThroughAnInheritedOverload` | yes | green |
| R5.5 | **The controller depends only on `<X>Api`**, never on a use case, a repository or the module's domain | INT | `ArchitectureRulesArchTest.deliveryNeverDependsOnUseCases` · `ArchitectureRulesArchTest.deliveryNeverDependsOnModuleDomain` | yes | green |
| R5.6 | **No JPA entity crosses to a controller.** Always a DTO | CON | `ModuleBoundariesArchTest.moduleInternalsAreOnlyAccessedWithinTheirModule` | yes | green |
| R5.7 | **`@Valid` on the controller is mandatory** whenever there is a request body | CON | `LayerContractArchTest.everyRequestBodyIsValidated` | yes | green |
| R5.8 | **Business validation lives in the use case**, not in the DTO and not in the controller. The DTO validates shape; the use case validates meaning | INT | `LayerContractArchTest.noBusinessRuleIsDecidedAtTheEdge` | yes | green |

---

## R6 — Database

The `N0.1`–`N9.2` rules live in [`database-rules.md`](database-rules.md), which is their long statement
and their reasoning. **They are not copied here** (R0.4). What this section declares is where each one
binds.

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R6.1 | **Every table has a primary key** (N1.1), link tables included | INT | `SchemaStandardIT.n1_1_everyTableHasAPrimaryKey` | yes | green |
| R6.2 | **The primary key of a link table is the pair of linked columns** (N1.3): the link *is* the pair, and a synthetic key on top admits two identical rows with different ids | INT | `SchemaStandardIT.n1_3_linkTablesAreKeyedByTheirPair` | yes | green |
| R6.3 | **A reference inside the same module carries a foreign key** (N2.1); one that crosses a module does not (R4.10) | INT | `SchemaStandardIT.n2_1_intraModuleReferencesCarryAForeignKey` | yes | green |
| R6.4 | **Every foreign key leads an index by its child column** (N2.3). Without it Oracle locks or scans the child table when the parent changes | COST | `SchemaStandardIT.n2_3_everyForeignKeyIsIndexed` | yes | green |
| R6.5 | **The delete action is explicit on every foreign key** (N2.4): cascade when the child does not exist without the parent, restrict when it has a life of its own. There is no default by omission | INT | `SchemaStandardIT.n2_4_everyForeignKeyDeclaresItsDeleteAction` | planned | pending |
| R6.6 | **An instant is `TIMESTAMP WITH TIME ZONE`** (N4.3). The system is cross-border by definition: a timestamp without a zone is ambiguous the moment a shipment crosses one | INT | `SchemaStandardIT.n4_3_timestampsCarryTimeZone` | yes | green |
| R6.7 | **A boolean is `NUMBER(1)` with `CHECK (col IN (0,1))`** (N4.4), **not** the native 23ai `BOOLEAN`. A function-based unique index whose expression touches a native `BOOLEAN` column materialises hidden virtual columns and freezes the table against schema change: on Oracle 23.26, dropping that index, dropping a column or reclaiming unused ones all answer `ORA-00600` — an internal engine error, with `SET UNUSED` as the only way out and no way back. None of this schema's three conditional indexes touches a boolean today; `HAS_OPEN_EXCURSION` is the obvious future candidate, and the point of a type rule is that it holds before somebody needs it. Hibernate maps `NUMBER(1)` to a Java `boolean` either way, so the cost is the type name in the dictionary and nothing else | INT | `SchemaStandardIT.n4_4_flagsAreNumberOneWithACheck` | yes | green |
| R6.8 | **Text is sized by its content** (N4.5), and a `CLOB` never enters an equality comparison, a `DISTINCT` or a `GROUP BY`: Oracle rejects it with `ORA-22848`, at runtime, not at compile time. `JSON` columns are read by key only | INT | `SchemaStandardIT.n4_5_noClobOrJsonInADistinctOrPredicate` | planned | pending |
| R6.9 | **Soft delete has one name in the whole schema** (N6.1): `DELETED_AT`, and `NULL` means alive. An instant and not a flag, because it answers "is it deleted?" and also "when?", which is the question asked the moment there is an incident. `REVOKED_AT`, `DETACHED_AT` and `SUPERSEDED_AT` are **not** soft delete: they are business states and they close a window rather than erase a row | INT | `SchemaStandardIT.n6_1_softDeleteHasOneName` | yes | green |
| R6.10 | **A unique index over a soft-deleted or state-scoped table is partial** (N7.3), expressed as a function-based index — `CASE WHEN cond THEN col END` — because Oracle has no partial indexes and does not index `NULL`s. It is what guarantees one active device assignment, one pending handoff and one current certificate | INT | `SchemaStandardIT.n7_3_conditionalUniquenessIsAFunctionBasedIndex` | yes | green |
| R6.11 | **Every business uniqueness rule exists as a constraint in the database** (N8.3), even when the code already validates it: the code validates to give a decent error message, the database guarantees. Without it, two concurrent requests pass both validations and both write | INT | `SchemaStandardIT.n8_3_businessUniquenessIsEnforcedByTheDatabase` | yes | green |
| R6.12 | **`NOT NULL` by default** (N8.1); nullable is the exception and means "this value may legitimately not exist". Mandatory text also carries `CHECK (TRIM(col) IS NOT NULL)`, because in Oracle the empty string **is** `NULL` and `NOT NULL` alone does not catch it | INT | `SchemaStandardIT.n8_1_mandatoryTextRejectsTheEmptyString` | yes | green |
| R6.13 | **A closed, stable set of values is a `CHECK`; one the business administers is a catalogue table with a foreign key** (N8.2). The criterion is who changes it: a deployment or a screen | INT | `SchemaStandardIT.n8_2_closedSetsAreChecks` | yes | green |
| R6.14 | **No constraint or index carries a system-generated name** (N9.1) | STYLE | `SchemaStandardIT.n9_1_noSystemGeneratedConstraintNames` | yes | green |
| R6.15 | **Every table carries the four audit columns** (N6.4) — `CREATED_AT`, `CREATED_BY`, `UPDATED_AT`, `UPDATED_BY` — and the first two are `NOT NULL` | INT | `SchemaStandardIT.n6_4_everyTableIsAuditable` | yes | green |
| R6.16 | **`TEMPERATURE_READING` is not partitioned.** Oracle refuses to partition, or to make unique, a `TIMESTAMP WITH TIME ZONE` column (ORA-02329, ORA-03001), and R6.6 wins: a cross-border instant without a zone is ambiguous, while a partition is an optimisation. Sample uniqueness is a function-based index on `SYS_EXTRACT_UTC(MEASURED_AT)`. **No machine:** there is nothing left to check | COST | — | none | — |

---

## R7 — Query and cost

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R7.1 | **The global `default_batch_fetch_size` is configured** and it is the one that rules | COST | `PersistenceFetchArchTest.theGlobalBatchFetchSizeIsConfigured` | yes | green |
| R7.2 | **Every listing endpoint has a query budget checked by exact equality**: one query more breaks it and one query less breaks it too. A budget that only has an upper bound stops noticing the day a read disappears because it started returning nothing | COST | `ShipmentListingQueryBudgetIT` · `TelemetrySeriesQueryBudgetIT` | planned | pending |
| R7.3 | **Aggregation is computed in the database, not by pulling rows into Java.** The compliance dashboard and the downsampled time series are `GROUP BY`, not streams | COST | `PersistenceFetchArchTest.aggregatesAreProjectedNotComputedInMemory` | yes | green |
| R7.4 | **A batch write is one statement, not one per row.** Reading ingestion writes thousands of rows at a time with application-generated UUIDs precisely so there is no round trip per row | COST | `TelemetryIngestionQueryBudgetIT` | planned | pending |
| R7.5 | **Pagination is `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY`**, never a nested `ROWNUM` | COST | `PersistenceFetchArchTest.paginationUsesTheAnsiOffsetSyntax` | yes | green |

---

## R8 — HTTP response, errors and messages

There are **exactly two response shapes** and both name the same concepts the same way, so a single
client function can read either.

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R8.1 | **Success:** an `ApiResponse` envelope with `status`, `code`, `messageKey`, `message`, `data`, `traceId`, `timestamp`, `meta` | CON | `HttpContractArchTest.theSuccessEnvelopeCarriesItsEightFields` | yes | green |
| R8.2 | **Error:** `application/problem+json` (RFC 9457) with `type`, `title`, `status`, `detail`, `instance`, `errorCode`, `messageKey`, `category`, `timestamp`, `traceId` | CON | `HttpContractArchTest.theErrorShapeIsProblemJsonPlusItsExtensions` | yes | green |
| R8.3 | **There is no `success` field.** The `status` already says it and the `Content-Type` tells the two shapes apart | CON | `HttpContractArchTest.thereIsNoSuccessFlagAndOnlyOneAsymmetry` | yes | green |
| R8.4 | **Every envelope field is always serialised**, `null` included. A typed client cannot depend on the *presence* of a field | CON | `HttpContractArchTest.everyEnvelopeFieldIsAlwaysSerialised` | yes | green |
| R8.5 | **Each module has its `<X>ErrorCode`** and the domain chooses the meaning, never the HTTP status | CON | `HttpContractArchTest.theDomainNeverNamesAnHttpStatus` | yes | green |
| R8.6 | **The HTTP status is decided by `ErrorCategory`**, a closed enum. A module does not pick an `HttpStatus` by hand | CON | `HttpContractArchTest.theHttpStatusOfAnErrorComesFromItsCategory` | yes | green |
| R8.7 | **`BUSINESS_RULE` is 422, not 500.** A business rule failure never goes out as an infrastructure error | CON | `HttpContractArchTest.theHttpStatusOfAnErrorComesFromItsCategory` | yes | green |
| R8.8 | **A use case throws `DomainException.of(<X>ErrorCode.Y)`.** Nobody returns an HTTP status from inside the domain | CON | `HttpContractArchTest.theDomainNeverNamesAnHttpStatus` | yes | green |
| R8.9 | **One single `GlobalExceptionHandler`** translates every exception into a `ProblemDetail`. There is no second translator | CON | `HttpContractArchTest.oneAdviceTranslatesEveryException` | yes | green |
| R8.10 | **The controller never assembles JSON by hand.** One line: `responseFactory.respond(code, mapper.toResponse(result))` | CON | `HttpContractArchTest.noControllerAssemblesItsOwnResponse` | yes | green |
| R8.11 | **The success HTTP status is carried by the `SuccessCode`**, not by the controller. The code names the operation and declares a `SuccessOutcome` — `CREATED`, `RETRIEVED`, `UPDATED`, `DELETED`, `ACCEPTED` — and the status comes from that closed enum, mirroring how `ErrorCategory` decides an error's status. A per-module enum of constants that each named their own status would let two modules disagree about what a delete returns, which is the thing this rule exists to prevent | CON | `HttpContractArchTest.noControllerChoosesAnHttpStatusItself` | yes | green |
| R8.12 | **Message keys follow a three-segment canon** in `snake_case`: `error.<module>.<reason>` and `success.<module>.<what>`. The key is the stable contract; the English text next to it is not | CON | `MessageKeyStyleTest.everyMessageKeyFollowsTheThreeSegmentCanon` | planned | pending |
| R8.13 | **The problem `type` is derived, never written**: from the `messageKey`, dropping the prefix and swapping separators (`error.shipment.handoff_expired` → `.../problems/shipment/handoff-expired`). A URL nobody types is a URL that cannot drift from the code | CON | `HttpContractArchTest.theProblemTypeIsDerivedAndNeverWritten` | yes | green |
| R8.14 | **A validation error adds `errors[]`** with field and reason; a rate-limit error adds `retryAfter` | CON | `HttpContractArchTest.validationAndRateLimitCarryTheirOwnExtensions` | yes | green |
| R8.15 | **Lists go through `responseFactory.paginated(...)`**, which fills `meta.pagination`. Every paginating endpoint has the same shape | CON | `HttpContractArchTest.paginationMetadataComesFromTheFactory` | yes | green |
| R8.16 | **One request identifier** (`traceId`) at the root of both shapes, and the `message`/`detail` asymmetry is the only one permitted — that name is fixed by RFC 9457 | CON | `HttpContractArchTest.bothShapesCarryTheSameTraceId` · `HttpContractArchTest.thereIsNoSuccessFlagAndOnlyOneAsymmetry` | yes | green |

### Proposals

| Id | Rule | Sev | Why |
|---|---|---|---|
| R8.17 | **Message keys resolve in English and Spanish** | CON | The project ships English only on purpose: the key is the contract and the text is not, so adding a locale later is additive. Promoting this needs a decision that the audience is bilingual, not a technical one |

---

## R9 — Security and tenancy

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R9.1 | **Everything is closed by default.** Public routes are declared in a single place (`SecurityConfig.permitAll`) and never with a permissive `@PreAuthorize`. A public route scattered around is one nobody can audit | SEC | `SecurityPostureArchTest.thePublicRoutesAreDeclaredInOnePlace` | yes | green |
| R9.2 | **Every endpoint is scope-guarded**, and authorization is applied on the method, not by route pattern in the configuration | SEC | `SecurityPostureArchTest.everyEndpointIsScopeGuarded` | yes | green |
| R9.3 | **The scope catalogue is a closed enum in code.** A new permission is a reviewable code change, not a row somebody inserts in production | SEC | `SecurityPostureArchTest.everyScopeDemandedByAnEndpointExistsInTheCatalogue` | yes | green |
| R9.4 | **The session is `STATELESS` and CSRF is disabled.** With no server-side session state there is nothing for CSRF to forge | SEC | `SecurityPostureArchTest.theFilterChainIsStatelessAndDropsCsrf` | yes | green |
| R9.5 | **An endpoint never takes the user or the organization from a header** and never reads the current user from an adapter. The filter resolves the organization context once, at the edge, and leaves it available | SEC | `SecurityPostureArchTest.endpointsDoNotTakeUserContextHeaders` | yes | green |
| R9.6 | **Every table holding organization data carries `ORGANIZATION_ID NOT NULL`** (N3.1). This is the deliberate exception to R4.10: tenancy carries a real foreign key even though it crosses a module, because it is the security property of the product and there declarative integrity beats decoupling | SEC | `SchemaStandardIT.n3_1_tenantColumnIsEnforced` | yes | green |
| R9.7 | **`ORGANIZATION_ID` leads an index** on every table queried by organization (N3.2) | COST | `SchemaStandardIT.n3_2_tenantColumnLeadsAnIndex` | yes | green |
| R9.8 | **Tenant filtering actually happens**, proven with data and not with structure. A structural test says the predicate is written somewhere; only data says a second organization gets nothing back | SEC | `TenantIsolationIT` | yes | green |
| R9.9 | **Visibility of a shipment is participation, and it is decided in one place** ([ADR-003](../docs/adr/ADR-003-visibility-by-participation.md)). The predicate lives in the repository, not in the service, so a new query cannot forget it | SEC | `SecurityPostureArchTest.shipmentVisibilityIsResolvedInOnePlace` · `ShipmentVisibilityIT` | planned | pending |
| R9.10 | **Asking for a resource you cannot see returns `404`, not `403`.** A `403` confirms the resource exists, and that is already leaking information to a third party | SEC | `ShipmentVisibilityIT` | planned | pending |
| R9.11 | **Passwords and client secrets are hashed with Argon2id.** Never reversible, never a fast hash | SEC | `SecurityPostureArchTest.secretsAreHashedWithArgon2` | yes | green |
| R9.12 | **Access token 15 minutes, refresh 7 days, and refresh rotation detects reuse**: using the same refresh token twice revokes the whole family | SEC | `SecurityPostureArchTest.theTokenLifetimesAreFifteenMinutesAndSevenDays` · `RefreshRotationIT` | yes | green |
| R9.13 | **A machine credential carries only the ingestion scope.** A compromised gateway cannot read a single shipment, and it never chooses its own organization: that travels signed in the token | SEC | `SecurityPostureArchTest.aMachineClientCannotAskForATenant` | yes | green |
| R9.14 | **Rate limiting on login, on token issuance and on ingestion** | SEC | `SecurityPostureArchTest.theUnauthenticatedEndpointsAreRateLimited` | yes | green |
| R9.15 | **Security headers are mandatory**: CSP `default-src 'none'`, `frame-ancestors 'none'`, HSTS one year, `Referrer-Policy`, `Permissions-Policy` | SEC | `SecurityPostureArchTest.everySecurityHeaderIsDeclared` | yes | green |
| R9.16 | **No secret is in the code or in a committed configuration file** | SEC | `SecurityPostureArchTest.noSecretIsWrittenIntoTheConfiguration` | yes | green |
| R9.17 | **Every denial is audited**, distinguishing an unauthenticated call from a denied scope, and goes out as a translated `ProblemDetail` | SEC | `SecurityPostureArchTest.everyDenialIsAudited` | planned | pending |
| R9.18 | **Every write leaves an `AUDIT_ENTRY`** with actor, resource and payload, written in the same transaction. If the audit write fails, the operation fails: auditing is not best-effort | SEC | `AuditTrailIT.everyWriteLeavesItsEntry` | yes | green |

---

## R10 — Transactions and events

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R10.1 | **The transaction is declared on the use case, never on the repository.** If the caller has no transaction, the missing use case gets created — the adapter does not get annotated | INT | `ArchitectureRulesArchTest.persistenceDeclaresNoTransactions` | yes | green |
| R10.2 | **`@Transactional` on writes only.** A query is not transactional unless it genuinely needs a consistent read, and then it is `readOnly` | COST | `LayerContractArchTest.queriesAreReadOnlyWhenTransactional` | yes | green |
| R10.3 | **Domain events run inside the publishing transaction.** If the reaction fails, the whole operation fails: there is no dispatched shipment with no monitoring window. The response is the last thing that happens | INT | `EventDeliveryArchTest.cascadesRunInsideThePublishingTransaction` | planned | pending |
| R10.4 | **An event handler is idempotent.** **No machine:** idempotence is proven by running the handler twice, not by reading it | INT | — | none | — |
| R10.5 | **An event lives in `api/event/`** and is part of the module's public contract | STYLE | `LayerContractArchTest.everyEventLivesInTheModulesApi` | yes | green |
| R10.6 | **No external call happens inside a database transaction.** The transaction would last as long as the third party does, holding locks meanwhile | COST | `LayerContractArchTest.noExternalCallRunsInsideATransaction` | yes | green |

---

## R11 — Concurrency

In force from day one. Every rule here describes a way of losing a value with nobody noticing.

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R11.1 | **Every entity two users can write at the same time carries `@Version`.** Without optimistic locking the last write wins in silence and the first is lost without a trace | INT | `ConcurrencyArchTest.everyConcurrentlyWritableEntityIsVersioned` | planned | pending |
| R11.2 | **No read-modify-write happens without a version or a lock.** `sequence_no` on the custody log is the canonical case: it is reserved inside the writing transaction with `SELECT MAX(...) + 1 FOR UPDATE`, never from an Oracle sequence, which would leave gaps on rollback | INT | `CustodySequenceConcurrencyIT.noTwoEventsShareASequenceNumber` | planned | pending |
| R11.3 | **A write the client can repeat is idempotent by key.** Batch ingestion is the canonical case: the same `idempotency_key` returns the original result and writes nothing | INT | `TelemetryIngestionIT.resendingABatchWritesNothing` | yes | green |
| R11.4 | **A concurrency conflict goes out as 409**, not as 500. It is an expected outcome, not a server failure | CON | `HttpContractArchTest.theHttpStatusOfAnErrorComesFromItsCategory` | yes | green |

---

## R12 — Observability

In force from day one. A log is replicated, exported and retained longer than the database itself.

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R12.1 | **No personal data and no secret goes into a log.** Not an email, not a token, not a password, not a document number — not even truncated | SEC | `ObservabilityArchTest.nothingSensitiveIsLogged` | planned | pending |
| R12.2 | **Every log line carries the request `traceId`** and is structured, not concatenated text. Without it, correlating a failure across layers is done by eye | COST | `ObservabilityArchTest.everyLogCarriesTheTraceId` | planned | pending |
| R12.3 | **`ERROR` is only what is actionable.** What obliges nobody to do anything is not an `ERROR`; an `ERROR` nobody attends to trains people not to look | COST | `ObservabilityArchTest.errorLevelIsReservedForTheActionable` | planned | pending |
| R12.4 | **No exception is swallowed in silence.** Either it is handled and explained, or it propagates. An empty `catch` turns a failure into a wrong value | INT | `ObservabilityArchTest.noCatchBlockIsEmpty` | planned | pending |
| R12.5 | **Log messages are constants in one place**, not string literals scattered at the call sites | STYLE | `ObservabilityArchTest.logMessagesComeFromOneCatalogue` | planned | pending |
| R12.6 | **Health and metrics are exposed** through Actuator, and the health endpoint is the only unauthenticated one among them | COST | `ObservabilityArchTest.onlyHealthIsPublicAmongTheActuatorEndpoints` | planned | pending |

---

## R13 — Schema, migrations and reference data

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R13.1 | **The schema is Flyway and only Flyway.** `ddl-auto: none` in every profile, tests included | INT | `SchemaLifecycleArchTest.hibernateNeverWritesTheSchemaInAnyProfile` | yes | green |
| R13.2 | **Migrations are hand-written, one per module, numbered and never edited once applied.** Hibernate does not generate interval partitioning, function-based indexes or composite `CHECK`s, and this schema depends on all three | INT | `SchemaLifecycleArchTest.everyMigrationIsNumberedAndUnique` | yes | green |
| R13.3 | **The dialect is pinned explicitly.** Without pinning it, the schema depends on the machine that produced it | INT | `SchemaLifecycleArchTest.theDialectIsPinnedExplicitly` | yes | green |
| R13.4 | **Reference data ships in the migration that creates its table** ([ADR-008](../docs/adr/ADR-008-reference-data-in-flyway.md)). No `CommandLineRunner`, no `@PostConstruct` writes rows, ever | INT | `SchemaLifecycleArchTest.nothingSeedsRowsAtStartup` | yes | green |
| R13.5 | **A catalogue that mirrors an enum is checked against it.** The code is the source of truth for the set, the migration for the rows, and a test asserts they agree. A mismatch fails the build; it is not silently repaired at boot | INT | `ScopeCatalogueIT.theSeededScopesMatchTheEnum` | yes | green |
| R13.6 | **Every migration is idempotent to re-run and the full rebuild is exercised**: drop, migrate, start, run the integration suite. It runs in CI, so "it works on a fresh database" is a fact and not a belief | INT | `rebuild` (Gradle task) | planned | pending |

---

## R14 — Documentation and module closure

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R14.1 | **A change that contradicts a rule corrects the rule in the same change.** A rule that lies is worse than no rule. **No machine:** whether a change contradicts a written rule is exactly what nobody knows how to automate | STYLE | — | none | — |
| R14.2 | **Every module has its build plan** in `docs/plan-0N-<module>.md`, and the plan's closing criterion is a command somebody can run | STYLE | `LayerContractArchTest.everyModuleHasItsBuildPlan` | yes | green |
| R14.3 | **A module is closed when** its rules in this catalogue are `green` or covered by a live waiver, and none of its area is still `pending`. Closure is a condition that is read from the repository, not a judgement — otherwise the bar moves with fatigue and the module built in week one is not held to what the one built in week four is | STYLE | `ModuleClosureTest.everyClosedModuleMeetsTheClosureConditions` | planned | pending |
| R14.4 | **One module at a time.** A finding in another module is written down and not fixed on the way past. It is not a preference about method: fixing everything at once is the reason nothing finishes | STYLE | `ModuleClosureTest.atMostOneModuleIsInProgress` | planned | pending |
| R14.5 | **Every public endpoint is described in OpenAPI**, generated from the code and not written by hand | CON | `HttpContractArchTest.everyEndpointIsDocumentedInOpenApi` | yes | green |

---

## R15 — Contract versioning

In force from day one. The version is already in the path (`/v1`); these rules say what may change
inside one.

| Id | Rule | Sev | Enforcer | Machine | Status |
|---|---|---|---|---|---|
| R15.1 | **Within a version, the contract only grows.** Adding an optional field is legal; removing one, renaming it, changing its type or narrowing its range is not. This is the operational definition of "do not break the client" | CON | `ContractVersionArchTest.theContractOnlyGrowsWithinAVersion` | planned | pending |
| R15.2 | **A breaking change opens a new version**, and the previous one stays alive while it has a consumer. **No machine:** whether a change breaks a client is a judgement about what the contract means, and "while it has a consumer" is a fact about the world outside this repository | CON | — | none | — |
| R15.3 | **Nothing is withdrawn without being announced first**: `@Deprecated` on the endpoint, a `Deprecation` header, and a declared sunset date | CON | `ContractVersionArchTest.everyDeprecatedEndpointAnnouncesItsSunset` | planned | pending |
| R15.4 | **A published `ErrorCode` never changes meaning.** A new one may be added; an existing one is not recycled. The client branches on that value | CON | `ContractVersionArchTest.noPublishedErrorCodeChangesItsMeaning` | planned | pending |

---

## Waiver register

Every tolerated red and every green that proves nothing, with **who accepted it and until when**
(R0.7). An expired waiver is a violation.

| Id | Rule | What is tolerated | Owner | Expires |
|---|---|---|---|---|
| — | — | *Empty. The project has no code yet; the first waiver will be the first compromise, and it will have a name on it.* | — | — |
