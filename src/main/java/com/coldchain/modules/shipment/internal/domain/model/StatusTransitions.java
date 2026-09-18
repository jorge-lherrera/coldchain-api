package com.coldchain.modules.shipment.internal.domain.model;

import com.coldchain.modules.shipment.api.ShipmentStatus;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class StatusTransitions {

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED =
            new EnumMap<>(ShipmentStatus.class);

    static {
        ALLOWED.put(ShipmentStatus.DRAFT, Set.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.CANCELLED));
        ALLOWED.put(ShipmentStatus.IN_TRANSIT,
                Set.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.AT_DESTINATION));
        ALLOWED.put(ShipmentStatus.AT_DESTINATION,
                Set.of(ShipmentStatus.DELIVERED, ShipmentStatus.REJECTED));
        ALLOWED.put(ShipmentStatus.DELIVERED, Set.of());
        ALLOWED.put(ShipmentStatus.REJECTED, Set.of());
        ALLOWED.put(ShipmentStatus.CANCELLED, Set.of());
    }

    private StatusTransitions() {
    }

    public static boolean allows(ShipmentStatus from, ShipmentStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static boolean terminal(ShipmentStatus status) {
        return ALLOWED.getOrDefault(status, Set.of()).isEmpty();
    }
}
