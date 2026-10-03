package com.soss.model.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * A delivery belongs to exactly one order.
 *
 * INTEGRATION NOTE: the shared Order and User entities do not exist in the base project yet.
 * Until they do, orderId and assignedStaffId are plain ID columns. When Order and User are
 * merged, replace them with @OneToOne Order and @ManyToOne User (role DELIVERY) and agree the
 * change with the group, because it affects the deliveries table.
 */
@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID of the order this delivery fulfils (one delivery per order). */
    @NotNull
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "delivery_address", nullable = false, length = 255)
    private String deliveryAddress;

    @Column(name = "expected_from")
    private LocalDateTime expectedFrom;

    @Column(name = "expected_to")
    private LocalDateTime expectedTo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStatus status = DeliveryStatus.CONFIRMED;

    /** ID of the delivery staff member (a User with role DELIVERY); null until assigned. */
    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Required by JPA. */
    protected Delivery() {
    }

    public Delivery(Long orderId, String deliveryAddress,
                    LocalDateTime expectedFrom, LocalDateTime expectedTo) {
        this.orderId = orderId;
        this.deliveryAddress = deliveryAddress;
        this.expectedFrom = expectedFrom;
        this.expectedTo = expectedTo;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public LocalDateTime getExpectedFrom() {
        return expectedFrom;
    }

    public LocalDateTime getExpectedTo() {
        return expectedTo;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public Long getAssignedStaffId() {
        return assignedStaffId;
    }

    public void setAssignedStaffId(Long assignedStaffId) {
        this.assignedStaffId = assignedStaffId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
