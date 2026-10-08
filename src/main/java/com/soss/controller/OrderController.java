package com.soss.controller;

import com.soss.model.Order;
import com.soss.model.OrderItem;
import com.soss.model.OrderStatus;
import com.soss.model.Payment;
import com.soss.model.User;
import com.soss.service.AccountService;
import com.soss.service.OrderCancellationService;
import com.soss.service.OrderReorderService;
import com.soss.service.OrderService;
import com.soss.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final AccountService accountService;
    private final OrderCancellationService cancellationService;
    private final OrderReorderService reorderService;

    public OrderController(
            OrderService orderService,
            PaymentService paymentService,
            AccountService accountService,
            OrderCancellationService cancellationService,
            OrderReorderService reorderService) {

        this.orderService = orderService;
        this.paymentService = paymentService;
        this.accountService = accountService;
        this.cancellationService = cancellationService;
        this.reorderService = reorderService;
    }

    /*
     * Customer-facing "My Orders" page.
     * Uses the currently logged-in account instead of requiring
     * the customer ID to be placed in the URL.
     */
    @GetMapping
    public String viewMyOrders(Principal principal, Model model) {

        var user = accountService.profile(principal.getName());

        List<Order> orders = orderService.getOrdersByCustomerId(user.getId());
        model.addAttribute("orders", orders);
        model.addAttribute("cancellableOrderIds", cancellableOrderIds(orders));

        model.addAttribute("customerId", user.getId());

        return "orders";
    }

    /*
     * JSON API - get one order.
     */
    @ResponseBody
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId, Principal principal) {
        return toResponse(requireOwnedOrder(orderId, principal));
    }

    /*
     * JSON API - get orders belonging to a customer.
     */
    @ResponseBody
    @GetMapping("/customer/{customerId}")
    public List<OrderResponse> getCustomerOrders(
            @PathVariable Long customerId,
            Principal principal) {

        requireCurrentCustomer(customerId, principal);

        return orderService
                .getOrdersByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * JSON API - create an order.
     */
    @ResponseBody
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Principal principal) {

        User customer = currentCustomer(principal);
        if (!Objects.equals(customer.getId(), request.customerId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Orders can only be created for the signed-in customer."
            );
        }

        Order order = new Order();

        order.setCustomerId(customer.getId());

        for (OrderItemRequest requestedItem : request.items()) {

            OrderItem item = new OrderItem();

            item.setProductId(requestedItem.productId());
            item.setProductName(requestedItem.productName());
            item.setQuantity(requestedItem.quantity());
            item.setUnitPrice(requestedItem.unitPrice());

            order.addItem(item);
        }

        Order savedOrder = orderService.createOrder(order);

        return ResponseEntity
                .created(URI.create("/orders/" + savedOrder.getId()))
                .body(toResponse(savedOrder));
    }

    /*
     * Existing customer-order page kept for compatibility.
     */
    @GetMapping("/customer/{customerId}/view")
    public String viewCustomerOrders(
            @PathVariable Long customerId,
            Principal principal,
            Model model) {

        requireCurrentCustomer(customerId, principal);

        List<Order> orders = orderService.getOrdersByCustomerId(customerId);
        model.addAttribute("orders", orders);
        model.addAttribute("cancellableOrderIds", cancellableOrderIds(orders));

        model.addAttribute("customerId", customerId);

        return "orders";
    }

    /*
     * Order confirmation page.
     */
    @GetMapping("/{orderId}/confirmation")
    public String viewOrderConfirmation(
            @PathVariable Long orderId,
            Principal principal,
            Model model) {

        Order order = requireOwnedOrder(orderId, principal);

        Payment latestPayment = paymentService
                .getPaymentsByOrderId(orderId)
                .stream()
                .max(
                        Comparator.comparing(
                                Payment::getPaymentDate,
                                Comparator.nullsFirst(
                                        Comparator.naturalOrder()
                                )
                        ).thenComparing(
                                Payment::getId,
                                Comparator.nullsFirst(
                                        Comparator.naturalOrder()
                                )
                        )
                )
                .orElse(null);

        model.addAttribute("order", order);
        model.addAttribute("payment", latestPayment);
        model.addAttribute("canCancel", cancellationService.canCancel(order));

        return "confirmation";
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable Long orderId, Principal principal,
            RedirectAttributes redirectAttributes) {
        User customer = currentCustomer(principal);
        requireOwnedOrder(orderId, principal);
        try {
            cancellationService.cancel(orderId, customer.getId());
            redirectAttributes.addFlashAttribute("message", "Order #" + orderId + " has been cancelled.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/orders";
    }

    @PostMapping("/{orderId}/reorder")
    public String reorderPreviousOrder(@PathVariable Long orderId, Principal principal,
            RedirectAttributes redirectAttributes) {
        User customer = currentCustomer(principal);
        // Keep the controller-level ownership response consistent with the other order actions.
        requireOwnedOrder(orderId, principal);
        try {
            var result = reorderService.reorder(orderId, customer.getId(), principal.getName());
            if (result.addedQuantity() > 0) {
                redirectAttributes.addFlashAttribute("message", "Added "
                        + result.addedQuantity() + " item"
                        + (result.addedQuantity() == 1 ? "" : "s") + " to your cart.");
            }
            if (!result.notices().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", String.join(" ", result.notices()));
            } else if (result.addedQuantity() == 0) {
                redirectAttributes.addFlashAttribute("error",
                        "None of the products in that order are currently available.");
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/cart";
    }

    /*
     * API error response.
     */
    @ResponseBody
    @ExceptionHandler(value = IllegalArgumentException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidOrder(
            IllegalArgumentException exception) {

        return Map.of(
                "error",
                exception.getMessage()
        );
    }

    @ResponseBody
    @ExceptionHandler(value = ResponseStatusException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> handleApiStatus(
            ResponseStatusException exception) {

        String message = exception.getReason() == null
                ? "Order request failed."
                : exception.getReason();

        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", message));
    }

    private OrderResponse toResponse(Order order) {

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getStatus()
        );
    }

    private User currentCustomer(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required.");
        }
        return accountService.profile(principal.getName());
    }

    private void requireCurrentCustomer(Long customerId, Principal principal) {
        if (!Objects.equals(currentCustomer(principal).getId(), customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your order history.");
        }
    }

    private Order requireOwnedOrder(Long orderId, Principal principal) {
        Order order;
        try {
            order = orderService.getOrderById(orderId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        requireCurrentCustomer(order.getCustomerId(), principal);
        return order;
    }

    private Set<Long> cancellableOrderIds(List<Order> orders) {
        return orders.stream()
                .filter(cancellationService::canCancel)
                .map(Order::getId)
                .collect(java.util.stream.Collectors.toSet());
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
