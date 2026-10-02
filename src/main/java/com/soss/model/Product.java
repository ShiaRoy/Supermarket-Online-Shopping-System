package com.soss.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long version;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 80)
    private String category;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    @Column(nullable = false, length = 32)
    private String icon = "basket";
    @Column(nullable = false)
    private boolean active = true;

    protected Product() {}
    public Product(String name, String category, String description, BigDecimal price, String icon) {
        update(name, category, description, price, icon, true);
    }
    public void update(String name, String category, String description, BigDecimal price, String icon, boolean active) {
        this.name = name; this.category = category; this.description = description;
        this.price = price; this.icon = icon; this.active = active;
    }
    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getIcon() { return icon; }
    public boolean isActive() { return active; }
}
