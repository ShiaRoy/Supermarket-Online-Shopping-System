package com.soss.service;

import com.soss.model.Order;
import com.soss.model.OrderItem;
import com.soss.model.OrderStatus;
import com.soss.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order createOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order must not be null");
        }
        if (order.getId() != null) {
            throw new IllegalArgumentException("A new order must not already have an ID");
        }
        if (order.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID must not be null");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            if (item == null) {
                throw new IllegalArgumentException("Order items must not be null");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Each item quantity must be greater than zero");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().signum() < 0) {
                throw new IllegalArgumentException("Each item unit price must not be null or negative");
            }
            totalAmount = totalAmount.add(item.getSubtotal());
        }

        for (OrderItem item : order.getItems()) {
            item.setOrder(order);
        }
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null");
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomerId(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID must not be null");
        }
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Order status must not be null");
        }
        Order order = getOrderById(orderId);
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
