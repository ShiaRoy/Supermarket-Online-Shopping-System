package com.soss.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "promotion")
public class Promotion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false) private Product product;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal percentOff;
    @Column(nullable = false) private LocalDate startsOn;
    @Column(nullable = false) private LocalDate endsOn;
    @Column(nullable = false) private boolean enabled;
    protected Promotion() {}
    public Promotion(Product product) { this.product = product; }
    public void update(String name, BigDecimal percentOff, LocalDate startsOn, LocalDate endsOn, boolean enabled) {
        this.name = name; this.percentOff = percentOff; this.startsOn = startsOn; this.endsOn = endsOn; this.enabled = enabled;
    }
    public boolean appliesOn(LocalDate day) {
        return enabled && product.isActive() && !day.isBefore(startsOn) && !day.isAfter(endsOn);
    }
    public String statusOn(LocalDate day) {
        if (!enabled) return "Disabled";
        if (!product.isActive()) return "Product hidden";
        if (day.isBefore(startsOn)) return "Scheduled";
        return day.isAfter(endsOn) ? "Expired" : "Active";
    }
    public BigDecimal discountedPrice() {
        return product.getPrice().multiply(BigDecimal.ONE.subtract(percentOff.movePointLeft(2)))
            .setScale(2, RoundingMode.HALF_UP).max(new BigDecimal("0.01"));
    }
    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public Product getProduct() { return product; }
    public String getName() { return name; }
    public BigDecimal getPercentOff() { return percentOff; }
    public LocalDate getStartsOn() { return startsOn; }
    public LocalDate getEndsOn() { return endsOn; }
    public boolean isEnabled() { return enabled; }
}
