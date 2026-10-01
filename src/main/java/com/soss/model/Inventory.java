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
    public String getStatus() { return quantity == 0 ? "Out of stock" : quantity <= 10 ? "Low stock" : "In stock"; }
    public String getStatusClass() { return quantity == 0 ? "out" : quantity <= 10 ? "low-stock" : "good"; }
}
