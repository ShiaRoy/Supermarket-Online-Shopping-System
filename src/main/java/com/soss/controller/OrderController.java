package com.soss.controller;

import com.soss.model.Order;
import com.soss.model.OrderItem;
import com.soss.model.OrderStatus;
import com.soss.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@ResponseBody
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return toResponse(orderService.getOrderById(orderId));
    }

    @GetMapping("/customer/{customerId}")
    public List<OrderResponse> getCustomerOrders(@PathVariable Long customerId) {
        return orderService.getOrdersByCustomerId(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerId(request.customerId());
        for (OrderItemRequest requestedItem : request.items()) {
            OrderItem item = new OrderItem();
            item.setProductId(requestedItem.productId());
            item.setProductName(requestedItem.productName());
            item.setQuantity(requestedItem.quantity());
            item.setUnitPrice(requestedItem.unitPrice());
            order.addItem(item);
        }
        Order savedOrder = orderService.createOrder(order);
        return ResponseEntity.created(URI.create("/orders/" + savedOrder.getId()))
                .body(toResponse(savedOrder));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidOrder(IllegalArgumentException exception) {
        return Map.of("error", exception.getMessage());
    }

    private OrderResponse toResponse(Order order) {
        // Keep responses independent of lazy collections and item back-references.
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getOrderDate(),
                order.getTotalAmount(), order.getStatus());
    }

    public record CreateOrderRequest(
            @NotNull Long customerId,
            @NotEmpty List<@NotNull @Valid OrderItemRequest> items) {
    }

    public record OrderItemRequest(
            Long productId,
            String productName,
            @NotNull @Positive Integer quantity,
            @NotNull @DecimalMin("0.00") BigDecimal unitPrice) {
    }

    public record OrderResponse(
            Long id,
            Long customerId,
            LocalDateTime orderDate,
            BigDecimal totalAmount,
            OrderStatus status) {
    }
}
