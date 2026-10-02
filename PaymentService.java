package com.soss.checkout;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Arrays;
import java.util.List;

/**
 * Placeholder only: no HTTP calls, credentials or simulated successful payments.
 * Replace this service when the real merchant API documentation is available.
 */
@Service
public class PaymentService {
    private final OrderRepository orders;

    public PaymentService(OrderRepository orders) { this.orders = orders; }

    public record MethodView(PaymentProvider provider, String name, boolean available, String status) {}
    public record PaymentView(Long orderId, String status, String integrationStatus) {}

    public List<MethodView> methods() {
        return Arrays.stream(PaymentProvider.values())
            .map(p -> new MethodView(p, p.displayName, false, "NOT_CONFIGURED")).toList();
    }

    private ShopOrder owned(Long id, String owner) {
        return orders.findByIdAndOwner(id, owner)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
    }

    public PaymentView status(Long id, String owner) {
        ShopOrder order = owned(id, owner);
        return new PaymentView(id, order.paymentStatus, "NOT_CONFIGURED");
    }

    public PaymentView start(Long id, String owner) {
        ShopOrder order = owned(id, owner);
        PaymentProvider provider = PaymentProvider.forMethod(order.paymentMethod);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            provider.displayName + " is an API placeholder. No payment was started or charged.");
    }
}
