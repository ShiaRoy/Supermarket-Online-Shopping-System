package com.soss.service.delivery;

import com.soss.model.delivery.Delivery;
import com.soss.model.delivery.DeliveryStatus;
import com.soss.repository.delivery.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    /**
     * Creates the delivery for an order. The Orders and Payments module calls this once an
     * order has been placed and paid (Payment -> Inventory Update -> Order -> Delivery).
     * A new delivery always starts as CONFIRMED.
     */
    @Transactional
    public Delivery createForOrder(Long orderId, String deliveryAddress,
                                   LocalDateTime expectedFrom, LocalDateTime expectedTo) {
        if (orderId == null) {
            throw new IllegalArgumentException("An order number is required");
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            throw new IllegalArgumentException("A delivery address is required");
        }
        if (deliveryRepository.findByOrderId(orderId).isPresent()) {
            throw new IllegalStateException("A delivery already exists for order " + orderId);
        }
        if (expectedFrom != null && expectedTo != null && expectedTo.isBefore(expectedFrom)) {
            throw new IllegalArgumentException("The delivery window cannot end before it starts");
        }
        return deliveryRepository.save(new Delivery(orderId, deliveryAddress, expectedFrom, expectedTo));
    }

    @Transactional(readOnly = true)
    public Optional<Delivery> findByOrderId(Long orderId) {
        return deliveryRepository.findByOrderId(orderId);
    }

    /** All deliveries, newest update first; pass a status to list only that stage. */
    @Transactional(readOnly = true)
    public List<Delivery> findAll(DeliveryStatus status) {
        return status == null
                ? deliveryRepository.findAllByOrderByUpdatedAtDesc()
                : deliveryRepository.findByStatusOrderByUpdatedAtDesc(status);
    }

    /**
     * Moves a delivery to its next stage. Only the next stage is accepted, so a delivery
     * cannot skip a stage, go backwards, or change after it has been delivered.
     */
    @Transactional
    public Delivery advanceStatus(Long deliveryId, DeliveryStatus requested) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery " + deliveryId + " not found"));

        DeliveryStatus current = delivery.getStatus();
        DeliveryStatus next = current.next();
        if (next == null) {
            throw new IllegalStateException("This delivery has already been delivered");
        }
        if (requested != next) {
            throw new IllegalStateException("A delivery can only move from "
                    + current.getLabel() + " to " + next.getLabel());
        }
        delivery.setStatus(next);
        return delivery;
    }

    /** Assigns delivery staff (the ID of a User with role DELIVERY) to a delivery; null removes the assignment. */
    @Transactional
    public Delivery assignStaff(Long deliveryId, Long staffId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery " + deliveryId + " not found"));
        delivery.setAssignedStaffId(staffId);
        return delivery;
    }
}
