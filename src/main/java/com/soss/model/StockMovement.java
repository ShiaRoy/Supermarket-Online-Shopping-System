package com.soss.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Immutable snapshots preserve history even when catalogue details change. */
@Entity
@Table(name = "stock_movement", indexes = @Index(name = "stock_history_lookup", columnList = "storeId,productId,occurredAt"))
public class StockMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, updatable = false) private Long productId;
    @Column(nullable = false, updatable = false) private Long storeId;
    @Column(nullable = false, updatable = false, length = 120) private String productName;
    @Column(nullable = false, updatable = false, length = 100) private String storeName;
    @Column(nullable = false, updatable = false) private int beforeQuantity;
    @Column(nullable = false, updatable = false) private int afterQuantity;
    @Column(nullable = false, updatable = false, length = 20) private String type;
    @Column(nullable = false, updatable = false, length = 250) private String reason;
    @Column(nullable = false, updatable = false, length = 255) private String actor;
    @Column(nullable = false, updatable = false) private Instant occurredAt;
    protected StockMovement() {}
    public StockMovement(Inventory item, int before, String type, String reason, String actor) {
        productId = item.getProduct().getId(); storeId = item.getStore().getId();
        productName = item.getProduct().getName(); storeName = item.getStore().getName();
        beforeQuantity = before; afterQuantity = item.getQuantity();
        this.type = type; this.reason = reason; this.actor = actor; occurredAt = Instant.now();
    }
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getStoreId() { return storeId; }
    public String getProductName() { return productName; }
    public String getStoreName() { return storeName; }
    public int getBeforeQuantity() { return beforeQuantity; }
    public int getAfterQuantity() { return afterQuantity; }
    public int getChange() { return afterQuantity - beforeQuantity; }
    public String getType() { return type; }
    public String getReason() { return reason; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
}
