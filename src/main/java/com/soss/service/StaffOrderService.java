package com.soss.service;

import com.soss.model.Order;
import com.soss.model.OrderStatus;
import com.soss.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operational order queue for staff; it intentionally exposes no payment credentials or refund actions. */
@Service
public class StaffOrderService {
    private final OrderRepository orders;

    public StaffOrderService(OrderRepository orders) { this.orders = orders; }

    public record OrderRow(Long id, Long customerId, LocalDateTime date, BigDecimal total,
                           OrderStatus status, String items) {}

    @Transactional(readOnly = true)
    public List<OrderRow> queue() {
        return orders.findAllByOrderByOrderDateDesc().stream()
                .map(order -> new OrderRow(order.getId(), order.getCustomerId(), order.getOrderDate(),
                        order.getTotalAmount(), order.getStatus(), order.getItems().stream()
                        .map(item -> item.getProductName() + " × " + item.getQuantity())
                        .collect(java.util.stream.Collectors.joining(", "))))
                .toList();
    }

    @Transactional
    public void markProcessing(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Order ID is required.");
        Order order = orders.lockByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Only a paid order can be marked as processing.");
        }
        order.setStatus(OrderStatus.PROCESSING);
        orders.save(order);
    }
}
