package com.coldchain.shared.paging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.shared.error.CoreErrorCode;
import com.coldchain.shared.error.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class SortCatalogTest {

    private enum ShipmentSortField implements SortField {
        DISPATCHED_AT("dispatchedAt", "dispatchedAt"),
        REFERENCE("reference", "reference"),
        ORIGIN("origin", "origin.name");

        private final String parameter;

        private final String property;

        ShipmentSortField(String parameter, String property) {
            this.parameter = parameter;
            this.property = property;
        }

        @Override
        public String parameter() {
            return parameter;
        }

        @Override
        public String property() {
            return property;
        }
    }

    private final SortCatalog<ShipmentSortField> catalog =
            SortCatalog.of(ShipmentSortField.class, ShipmentSortField.DISPATCHED_AT, Sort.Direction.DESC);

    @Test
    void anUnsortedRequestFallsBackToTheDeclaredDefault() {
        assertThat(catalog.resolve(Sort.unsorted())).isEqualTo(Sort.by(Sort.Direction.DESC, "dispatchedAt"));
    }

    @Test
    void aDeclaredFieldIsTranslatedToItsPersistenceProperty() {
        Sort resolved = catalog.resolve(Sort.by(Sort.Direction.ASC, "origin"));

        assertThat(resolved).isEqualTo(Sort.by(Sort.Direction.ASC, "origin.name"));
    }

    @Test
    void anUndeclaredFieldIsRejectedInsteadOfReachingTheDatabase() {
        assertThatThrownBy(() -> catalog.resolve(Sort.by("passwordHash")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("passwordHash")
                .hasMessageContaining("dispatchedAt")
                .extracting(exception -> ((DomainException) exception).errorCode())
                .isEqualTo(CoreErrorCode.UNSORTABLE_FIELD);
    }

    @Test
    void applyingToAPageableKeepsThePageAndTheSizeAndOnlyRewritesTheSort() {
        Pageable applied = catalog.apply(PageRequest.of(3, 50, Sort.by("reference")));

        assertThat(applied.getPageNumber()).isEqualTo(3);
        assertThat(applied.getPageSize()).isEqualTo(50);
        assertThat(applied.getSort()).isEqualTo(Sort.by("reference"));
    }

    @Test
    void everyDeclaredFieldIsAcceptedByTheCatalogueThatDeclaredIt() {
        for (ShipmentSortField field : ShipmentSortField.values()) {
            assertThat(catalog.resolve(Sort.by(field.parameter())))
                    .isEqualTo(Sort.by(field.property()));
        }
    }
}
