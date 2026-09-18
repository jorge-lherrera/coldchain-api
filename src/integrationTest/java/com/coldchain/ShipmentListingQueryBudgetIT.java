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
        RegisterOrganizationResult few = fixtures.organization("few");
        RegisterOrganizationResult many = fixtures.organization("many");
        fixtures.draftShipment(few);
        fixtures.draftShipment(many);
        fixtures.draftShipment(many);
        fixtures.draftShipment(many);

        int spentOnOne = spentListing(few);
        int spentOnThree = spentListing(many);

        assertThat(spentOnThree)
                .describedAs("three rows cost more queries than one, which is the shape of an N+1 "
                        + "and it grows with the tenant")
                .isEqualTo(spentOnOne);
        assertThat(spentOnThree)
                .describedAs("a budget with only an upper bound stops noticing the day a read "
                        + "disappears because it started returning nothing")
                .isEqualTo(BUDGET);
    }

    private int spentListing(RegisterOrganizationResult owner) {
        QueryCounter.reset();
        PagedResult<ShipmentResult> page = shipments.listShipments(owner.organizationId(), FIRST_PAGE);
        assertThat(page.content()).isNotEmpty();
        return QueryCounter.counted();
    }
}
