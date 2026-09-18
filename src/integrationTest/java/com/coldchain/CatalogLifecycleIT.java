package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.modules.catalog.api.CatalogApi;
import com.coldchain.modules.catalog.api.ProfileStatus;
import com.coldchain.modules.catalog.api.dto.CreateProductCommand;
import com.coldchain.modules.catalog.api.dto.CreateStorageProfileCommand;
import com.coldchain.modules.catalog.api.dto.ProductResult;
import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.catalog.api.dto.StorageThresholds;
import com.coldchain.modules.catalog.api.dto.UpdateStorageProfileCommand;
import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.modules.identity.api.OrganizationKind;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationCommand;
import com.coldchain.shared.error.DomainException;
import com.coldchain.shared.identifier.RawUuid;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class CatalogLifecycleIT {

    private static final StorageThresholds FRIDGE = new StorageThresholds(
            new BigDecimal("2.00"), new BigDecimal("8.00"), 30, 120, new BigDecimal("95.00"));

    @Autowired
    private CatalogApi catalog;

    @Autowired
    private IdentityApi identity;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void anActiveProfileRefusesToBeEditedAndIsClonedInstead() {
        UUID organization = organization();
        StorageProfileResult draft = catalog.createStorageProfile(
                new CreateStorageProfileCommand(organization, code(), "Fridge 2-8", FRIDGE));
        catalog.activateStorageProfile(draft.id());

        assertThatThrownBy(() -> catalog.updateStorageProfile(
                new UpdateStorageProfileCommand(draft.id(), "Fridge 2-8 v2", FRIDGE)))
                .describedAs("a shipment already copied these thresholds, so they cannot move")
                .isInstanceOf(DomainException.class)
                .extracting(failure -> ((DomainException) failure).errorCode().name())
                .isEqualTo("PROFILE_IN_USE_CLONE_INSTEAD");

        StorageProfileResult next = catalog.cloneStorageProfileToNextVersion(draft.id());

        assertThat(next.version()).isEqualTo(2);
        assertThat(next.status()).isEqualTo(ProfileStatus.DRAFT);
        assertThat(catalog.listStorageProfiles(organization, Pages.FIRST).content())
                .extracting(StorageProfileResult::status)
                .containsExactlyInAnyOrder(ProfileStatus.RETIRED, ProfileStatus.DRAFT);
    }

    @Test
    void aRetiredSkuCanBeUsedAgainButALiveOneCannot() {
        UUID organization = organization();
        UUID profile = activeProfile(organization);
        String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8);
        ProductResult first = catalog.createProduct(
                new CreateProductCommand(organization, sku, "Vaccine A", profile));

        assertThatThrownBy(() -> catalog.createProduct(
                new CreateProductCommand(organization, sku, "Vaccine B", profile)))
                .describedAs("the database is what makes the SKU unique, not an if in the service")
                .isInstanceOf(DomainException.class)
                .extracting(failure -> ((DomainException) failure).errorCode().name())
                .isEqualTo("SKU_ALREADY_IN_USE");

        catalog.retireProduct(first.id());

        assertThat(catalog.createProduct(
                new CreateProductCommand(organization, sku, "Vaccine B", profile)).sku())
                .describedAs("a retired row falls out of the function-based index and frees its SKU")
                .isEqualTo(sku);
    }

    @Test
    void theDatabaseRejectsAnUpsideDownRange() {
        UUID organization = organization();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO storage_profile (id, organization_id, code, name, profile_version, status,
                        min_celsius, max_celsius, max_single_excursion_min,
                        max_cumulative_excursion_min, min_coverage_pct, created_at, created_by)
                VALUES (SYS_GUID(), ?, 'UPSIDE-DOWN', 'Upside down', 1, 'DRAFT',
                        8, 2, 10, 20, 95, SYSTIMESTAMP, SYS_GUID())
                """, RawUuid.toBytes(organization)))
                .describedAs("two concurrent requests can dodge an if, they cannot dodge a CHECK")
                .hasMessageContaining("CK_STORAGE_PROFILE_RANGE");
    }

    @Test
    void aProductCannotPointAtADraftProfile() {
        UUID organization = organization();
        StorageProfileResult draft = catalog.createStorageProfile(
                new CreateStorageProfileCommand(organization, code(), "Still a draft", FRIDGE));

        assertThatThrownBy(() -> catalog.createProduct(new CreateProductCommand(organization,
                "SKU-" + UUID.randomUUID().toString().substring(0, 8), "Too early", draft.id())))
                .isInstanceOf(DomainException.class)
                .extracting(failure -> ((DomainException) failure).errorCode().name())
                .isEqualTo("PROFILE_NOT_USABLE");
    }

    @Test
    void aCatalogIsOnlyVisibleToTheOrganizationThatOwnsIt() {
        UUID mine = organization();
        UUID theirs = organization();
        activeProfile(mine);

        assertThat(catalog.listStorageProfiles(theirs, Pages.FIRST).content())
                .describedAs("a catalogue that leaks is a price list handed to a competitor")
                .isEmpty();
        assertThat(catalog.listStorageProfiles(mine, Pages.FIRST).content()).hasSize(1);
    }

    private UUID activeProfile(UUID organization) {
        StorageProfileResult draft = catalog.createStorageProfile(
                new CreateStorageProfileCommand(organization, code(), "Fridge 2-8", FRIDGE));
        return catalog.activateStorageProfile(draft.id()).id();
    }

    private UUID organization() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return identity.registerOrganization(new RegisterOrganizationCommand(
                "TAX-" + suffix, "Catalog Labs S.A.", "Catalog", OrganizationKind.LAB, "UY",
                "admin-" + suffix + "@catalog.test", "Catalog Admin", "Catalog!Secret42"))
                .organizationId();
    }

    private static String code() {
        return "PROF-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
