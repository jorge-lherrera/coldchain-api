package com.coldchain.modules.shipment.internal.infrastructure.persistence.entity;

import com.coldchain.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "SHIPMENT_LINE", indexes = {
        @Index(name = "IX_SHIPMENT_LINE_SHIPMENT_ID", columnList = "SHIPMENT_ID")})
public class ShipmentLineJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "SHIPMENT_ID", nullable = false)
    private UUID shipmentId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private UUID productId;

    @Column(name = "PRODUCT_NAME", nullable = false)
    private String productName;

    @Column(name = "PROFILE_VERSION", nullable = false)
    private int profileVersion;

    @Column(name = "QUANTITY", nullable = false)
    private BigDecimal quantity;

    @Column(name = "UNIT", nullable = false)
    private String unit;

    protected ShipmentLineJpaEntity() {
    }

    public ShipmentLineJpaEntity(UUID id, UUID shipmentId, UUID productId, String productName, int profileVersion, BigDecimal quantity, String unit) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.productId = productId;
        this.productName = productName;
        this.profileVersion = profileVersion;
        this.quantity = quantity;
        this.unit = unit;
    }

    public UUID getId() {
        return id;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getProfileVersion() {
        return profileVersion;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }
}
