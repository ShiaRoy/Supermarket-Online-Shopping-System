package com.soss.service;

import com.soss.model.Order;
import com.soss.model.OrderItem;
import com.soss.model.OrderStatus;
import com.soss.repository.OrderRepository;
import com.soss.service.delivery.DeliveryService;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates the reversible checkout work when a customer cancels an eligible order. */
@Service
public class OrderCancellationService {

    private final OrderRepository orders;
    private final CatalogueService catalogue;
    private final DeliveryService deliveryService;

    public OrderCancellationService(OrderRepository orders, CatalogueService catalogue,
            DeliveryService deliveryService) {
        this.orders = orders;
        this.catalogue = catalogue;
        this.deliveryService = deliveryService;
    }

    @Transactional
    public Order cancel(Long orderId, Long customerId) {
        if (orderId == null || customerId == null) {
            throw new IllegalArgumentException("Order and customer are required.");
        }
        Order order = orders.lockByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));
        if (!Objects.equals(order.getCustomerId(), customerId)) {
            throw new IllegalArgumentException("You can only cancel your own orders.");
        }
        validateOrderStatus(order);

        // Rechecks delivery eligibility under a delivery lock before state is committed.
        deliveryService.cancelForOrder(orderId);

        if (order.isInventoryDeducted()) {
            Long storeId = catalogue.store(null).getId();
            for (OrderItem item : order.getItems()) {
                catalogue.restoreStock(item.getProductId(), storeId, item.getQuantity());
            }
            order.setInventoryDeducted(false);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orders.save(order);
    }

    public boolean canCancel(Order order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            return false;
        }
        return deliveryService.findByOrderId(order.getId())
                .map(delivery -> delivery.getStatus() != com.soss.model.delivery.DeliveryStatus.OUT_FOR_DELIVERY
                        && delivery.getStatus() != com.soss.model.delivery.DeliveryStatus.DELIVERED
                        && delivery.getStatus() != com.soss.model.delivery.DeliveryStatus.CANCELLED)
                .orElse(true);
    }

    private void validateOrderStatus(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("This order has already been cancelled.");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Completed orders cannot be cancelled.");
        }
    }
}
