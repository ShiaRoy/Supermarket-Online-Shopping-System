package com.soss.model;

import com.soss.model.Product;
import jakarta.persistence.*;

@Entity
@Table(name = "inventory", uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "product_id"}))
public class Inventory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false, columnDefinition = "integer default 10")
    private int lowStockThreshold = 10;
    protected Inventory() {}
    public Inventory(Store store, Product product, int quantity) {
        this.store = store; this.product = product; setQuantity(quantity);
    }
    public void setQuantity(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        this.quantity = quantity;
    }
    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public Store getStore() { return store; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int threshold) {
        if (threshold < 0 || threshold > 1000000) throw new IllegalArgumentException("Invalid stock threshold.");
        this.lowStockThreshold = threshold;
    }
    public boolean isLowStock() { return quantity <= lowStockThreshold; }
    public String getStatus() { return quantity == 0 ? "Out of stock" : isLowStock() ? "Low stock" : "In stock"; }
    public String getStatusClass() { return quantity == 0 ? "out" : isLowStock() ? "low-stock" : "good"; }
}
