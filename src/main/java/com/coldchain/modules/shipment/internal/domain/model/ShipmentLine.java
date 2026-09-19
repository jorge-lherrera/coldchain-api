package com.coldchain.modules.shipment.internal.domain.model;

import com.coldchain.shared.util.UuidV7;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public final class ShipmentLine {

    private final UUID id;

    private final UUID shipmentId;

    private final UUID productId;

    private final String productName;

    private final int profileVersion;

    private final BigDecimal quantity;

    private final String unit;

    private ShipmentLine(UUID id, UUID shipmentId, UUID productId, String productName,
            int profileVersion, BigDecimal quantity, String unit) {
        this.id = Objects.requireNonNull(id);
        this.shipmentId = Objects.requireNonNull(shipmentId);
        this.productId = Objects.requireNonNull(productId);
        this.productName = Objects.requireNonNull(productName);
        this.profileVersion = profileVersion;
        this.quantity = Objects.requireNonNull(quantity);
        this.unit = Objects.requireNonNull(unit);
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("A line that carries nothing is not a line");
        }
    }

    public static ShipmentLine createNew(UUID shipmentId, UUID productId, String productName,
            int profileVersion, BigDecimal quantity, String unit) {
        return new ShipmentLine(UuidV7.generate(), shipmentId, productId, productName, profileVersion,
                quantity, unit);
    }

    public static ShipmentLine restore(UUID id, UUID shipmentId, UUID productId, String productName,
            int profileVersion, BigDecimal quantity, String unit) {
        return new ShipmentLine(id, shipmentId, productId, productName, profileVersion, quantity, unit);
    }

    public UUID id() {
        return id;
    }

    public UUID shipmentId() {
        return shipmentId;
    }

    public UUID productId() {
        return productId;
    }

    public String productName() {
        return productName;
    }

    public int profileVersion() {
        return profileVersion;
    }

    public BigDecimal quantity() {
        return quantity;
    }

    public String unit() {
        return unit;
    }
}
