package com.soss.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(
        name = "uk_orders_customer_checkout_token", columnNames = {"customer_id", "checkout_token"}))
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;

    @Column(name = "checkout_token", length = 36)
    private String checkoutToken;

    private LocalDateTime orderDate;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal subtotalAmount;
    @Column(precision = 19, scale = 2)
    private BigDecimal deliveryAmount;
    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount;
    @Column(length = 32)
    private String promoCode;

    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal value) { subtotalAmount = value; }
    public BigDecimal getDeliveryAmount() { return deliveryAmount; }
    public void setDeliveryAmount(BigDecimal value) { deliveryAmount = value; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal value) { discountAmount = value; }
    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String value) { promoCode = value; }

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void applyDefaults() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
        if (status == null) {
            status = OrderStatus.PENDING_PAYMENT;
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }

    public void addItem(OrderItem item) {
        Objects.requireNonNull(item, "Order item must not be null");
        if (!items.contains(item)) {
            items.add(item);
        }
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        if (items.remove(item)) {
            item.setOrder(null);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCheckoutToken() {
        return checkoutToken;
    }

    public void setCheckoutToken(String checkoutToken) {
        this.checkoutToken = checkoutToken;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        List<OrderItem> replacement = new ArrayList<>(
                Objects.requireNonNull(items, "Order items must not be null"));
        replacement.forEach(item -> Objects.requireNonNull(item, "Order item must not be null"));
        // Preserve Hibernate's managed collection when replacing its contents.
        for (OrderItem item : new ArrayList<>(this.items)) {
            removeItem(item);
        }
        replacement.forEach(this::addItem);
    }
}
