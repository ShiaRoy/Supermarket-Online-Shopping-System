package com.soss.service.delivery;

import com.soss.model.Role;
import com.soss.model.User;
import com.soss.model.delivery.Delivery;
import com.soss.model.delivery.DeliveryStatus;
import com.soss.repository.UserRepository;
import com.soss.repository.delivery.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;

    public DeliveryService(DeliveryRepository deliveryRepository, UserRepository userRepository) {
        this.deliveryRepository = deliveryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates the delivery for an order. 
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

    /** Only the deliveries assigned to a specific delivery account. */
    @Transactional(readOnly = true)
    public List<Delivery> findAssignedToStaff(Long staffId, DeliveryStatus status) {
        if (staffId == null) {
            throw new IllegalArgumentException("A delivery account is required.");
        }
        return status == null
                ? deliveryRepository.findByAssignedStaffIdOrderByUpdatedAtDesc(staffId)
                : deliveryRepository.findByAssignedStaffIdAndStatusOrderByUpdatedAtDesc(staffId, status);
    }

    @Transactional(readOnly = true)
    public Delivery getDelivery(Long deliveryId) {
        return findDelivery(deliveryId);
    }

    /**
     * Moves a delivery to its next stage. 
     */
    @Transactional
    public Delivery advanceStatus(Long deliveryId, DeliveryStatus requested, String receivedBy) {
        Delivery delivery = findDelivery(deliveryId);
        if (delivery.getCancelledAt() != null) {
            throw new IllegalStateException("This delivery has been cancelled");
        }

        DeliveryStatus current = delivery.getStatus();
        DeliveryStatus next = current.next();
        if (next == null) {
            throw new IllegalStateException("This delivery has already been delivered");
        }
        if (requested != next) {
            throw new IllegalStateException("A delivery can only move from "
                    + current.getLabel() + " to " + next.getLabel());
        }

        LocalDateTime now = LocalDateTime.now();
        if (next == DeliveryStatus.PACKED) {
            delivery.setPackedAt(now);
        } else if (next == DeliveryStatus.OUT_FOR_DELIVERY) {
            if (delivery.getAssignedStaffId() == null) {
                throw new IllegalStateException("Assign a driver before sending this delivery out");
            }
            delivery.setOutForDeliveryAt(now);
            // A new attempt starts, so the note about the last failed attempt is cleared.
            delivery.setFailedAttemptReason(null);
            delivery.setFailedAttemptAt(null);
        } else if (next == DeliveryStatus.DELIVERED) {
            if (receivedBy == null || receivedBy.isBlank()) {
                throw new IllegalStateException("Enter who received the order");
            }
            if (receivedBy.trim().length() > 100) {
                throw new IllegalArgumentException("The name is too long (maximum 100 characters)");
            }
            delivery.setReceivedBy(receivedBy.trim());
            delivery.setDeliveredAt(now);
        }
        delivery.setStatus(next);
        return delivery;
    }

    /**
     * The driver could not hand over the order. The delivery goes back to Packed with the
     * reason, so staff can send it out again.
     */
    @Transactional
    public Delivery reportFailedAttempt(Long deliveryId, String reason) {
        Delivery delivery = findDelivery(deliveryId);
        if (delivery.getCancelledAt() != null) {
            throw new IllegalStateException("This delivery has been cancelled");
        }
        if (delivery.getStatus() != DeliveryStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException("Only a delivery that is out for delivery can have a failed attempt");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Enter the reason for the failed attempt");
        }
        if (reason.trim().length() > 255) {
            throw new IllegalArgumentException("The reason is too long (maximum 255 characters)");
        }
        delivery.setStatus(DeliveryStatus.PACKED);
        delivery.setOutForDeliveryAt(null);
        delivery.setFailedAttemptReason(reason.trim());
        delivery.setFailedAttemptAt(LocalDateTime.now());
        return delivery;
    }

    /** The customer confirms they received the order. Only possible once it is Delivered. */
    @Transactional
    public Delivery confirmReceived(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("No delivery for order " + orderId));
        if (delivery.getStatus() != DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("This order has not been delivered yet");
        }
        if (delivery.getCustomerConfirmedAt() == null) {
            delivery.setCustomerConfirmedAt(LocalDateTime.now());
        }
        return delivery;
    }



    private Delivery findDelivery(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery " + deliveryId + " not found"));
    }

    /**
     this part is for Assign delivery staff 
     */
    @Transactional
    public Delivery assignStaff(Long deliveryId, Long staffId) {
        Delivery delivery = findDelivery(deliveryId);
        if (delivery.getCancelledAt() != null) {
            throw new IllegalStateException("This delivery has been cancelled");
        }
        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("This delivery has already been delivered");
        }
        if (staffId == null && delivery.getStatus() == DeliveryStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException("A delivery that is out for delivery needs a driver");
        }
        if (staffId != null) {
            boolean isDriver = userRepository.findById(staffId)
                    .map(user -> user.getRole() == Role.DELIVERY)
                    .orElse(false);
            if (!isDriver) {
                throw new IllegalArgumentException("Choose a delivery driver from the list");
            }
        }
        delivery.setAssignedStaffId(staffId);
        return delivery;
    }

    /**
     * Cancels an eligible delivery as part of an order cancellation. A delivery that is
     * already out for delivery or delivered cannot be cancelled. Missing deliveries are valid.
     * The pessimistic lock keeps this check atomic with the order cancellation transaction.
     */
    @Transactional
    public Optional<Delivery> cancelForOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("An order number is required");
        }
        Optional<Delivery> found = deliveryRepository.lockByOrderId(orderId);
        if (found.isEmpty()) {
            return found;
        }
        Delivery delivery = found.get();
        if (delivery.getCancelledAt() != null) {
            return found;
        }
        DeliveryStatus status = delivery.getStatus();
        if (status == DeliveryStatus.OUT_FOR_DELIVERY || status == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("A delivery that is on its way or delivered cannot be cancelled");
        }
        delivery.setCancelledAt(LocalDateTime.now());
        return found;
    }

    /** The accounts with the DELIVERY role, sorted by name, for the driver dropdown. */
    @Transactional(readOnly = true)
    public List<User> findDrivers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.DELIVERY)
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Driver ID to driver name, so the manage page can show a name instead of a number. */
    @Transactional(readOnly = true)
    public Map<Long, String> driverNames() {
        Map<Long, String> names = new HashMap<>();
        for (User driver : findDrivers()) {
            names.put(driver.getId(), driver.getFullName());
        }
        return names;
    }
}
