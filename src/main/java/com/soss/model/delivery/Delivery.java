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


@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
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

    /** this part is forr  the delivery staff  null until assigned. */
    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // this part is for the  steps on the tracking page.
    @Column(name = "packed_at")
    private LocalDateTime packedAt;

    @Column(name = "out_for_delivery_at")
    private LocalDateTime outForDeliveryAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    
    @Size(max = 100)
    @Column(name = "received_by", length = 100)
    private String receivedBy;

    @Column(name = "customer_confirmed_at")
    private LocalDateTime customerConfirmedAt;

    // this is for the  set when the driver could not complete a delivery attempt.
    @Size(max = 255)
    @Column(name = "failed_attempt_reason", length = 255)
    private String failedAttemptReason;

    @Column(name = "failed_attempt_at")
    private LocalDateTime failedAttemptAt;

    // this for the set when the order is cancelled before it goes out for delivery.
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

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

    public LocalDateTime getPackedAt() {
        return packedAt;
    }

    public void setPackedAt(LocalDateTime packedAt) {
        this.packedAt = packedAt;
    }

    public LocalDateTime getOutForDeliveryAt() {
        return outForDeliveryAt;
    }

    public void setOutForDeliveryAt(LocalDateTime outForDeliveryAt) {
        this.outForDeliveryAt = outForDeliveryAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public LocalDateTime getCustomerConfirmedAt() {
        return customerConfirmedAt;
    }

    public void setCustomerConfirmedAt(LocalDateTime customerConfirmedAt) {
        this.customerConfirmedAt = customerConfirmedAt;
    }

    public String getFailedAttemptReason() {
        return failedAttemptReason;
    }

    public void setFailedAttemptReason(String failedAttemptReason) {
        this.failedAttemptReason = failedAttemptReason;
    }

    public LocalDateTime getFailedAttemptAt() {
        return failedAttemptAt;
    }

    public void setFailedAttemptAt(LocalDateTime failedAttemptAt) {
        this.failedAttemptAt = failedAttemptAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
