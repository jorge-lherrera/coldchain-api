package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.shipment.api.ShipmentApi;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import com.coldchain.shared.paging.SortDirection;
import com.coldchain.shared.paging.SortOrder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@IntegrationTest
@Import(QueryCounter.class)
class ShipmentListingQueryBudgetIT {

    private static final int BUDGET = 2;

    private static final PageCriteria FIRST_PAGE =
            new PageCriteria(0, 20, List.of(new SortOrder("reference", SortDirection.ASC)));

    @Autowired
    private ShipmentApi shipments;

    @Autowired
    private Fixtures fixtures;

    @Test
    void listingThreeShipmentsCostsTheSameAsListingOne() {
        RegisterOrganizationResult owner = fixtures.organization("budget");
        fixtures.draftShipment(owner);
        fixtures.draftShipment(owner);
        fixtures.draftShipment(owner);

        QueryCounter.reset();
        PagedResult<ShipmentResult> page = shipments.listShipments(owner.organizationId(), FIRST_PAGE);
        int spent = QueryCounter.counted();

        assertThat(page.content()).hasSize(3);
        assertThat(spent)
                .describedAs("a budget with only an upper bound stops noticing the day a read "
                        + "disappears because it started returning nothing")
                .isEqualTo(BUDGET);
    }
}
