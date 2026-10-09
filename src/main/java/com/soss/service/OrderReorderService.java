package com.soss.service;

import com.soss.model.Order;
import com.soss.repository.OrderRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates an ownership-checked reorder without changing the original order. */
@Service
public class OrderReorderService {

    private final OrderRepository orders;
    private final CartService carts;

    public OrderReorderService(OrderRepository orders, CartService carts) {
        this.orders = orders;
        this.carts = carts;
    }

    @Transactional
    public CartService.ReorderResult reorder(Long orderId, Long customerId, String ownerEmail) {
        if (orderId == null || customerId == null || ownerEmail == null || ownerEmail.isBlank()) {
            throw new IllegalArgumentException("Order and customer are required.");
        }

        Order order = orders.lockByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));
        if (!Objects.equals(order.getCustomerId(), customerId)) {
            throw new IllegalArgumentException("You can only reorder your own orders.");
        }

        List<CartService.ReorderLine> lines = order.getItems().stream()
                .map(item -> new CartService.ReorderLine(
                        item.getProductId(), item.getProductName(), item.getQuantity()))
                .toList();
        return carts.reorder(ownerEmail, lines);
    }
}
