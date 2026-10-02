package com.soss.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Cart-owned integration contract, not an order entity/repository or payment service.
 * Implement using the shared Orders module. Both methods must use the authenticated owner.
 */
public interface CheckoutOrders {
    record Receipt(Long orderId) {}
    record Item(Long productId, String name, int quantity, BigDecimal unitPrice, BigDecimal total) {}
    record Request(String owner, String checkoutToken, String address, String deliverySlot,
                   String paymentMethod, String currency, BigDecimal subtotal, BigDecimal delivery,
                   BigDecimal discount, BigDecimal total, String promo, List<Item> items) {}

    Optional<Receipt> findExisting(String owner, String checkoutToken);

    /**
     * Persist the shared order/items idempotently by owner + checkoutToken.
     * Join CartService's transaction using the SAME datasource/transaction manager.
     * Return only after persistence succeeds; propagate failures so the cart rolls back.
     * A remote Orders service instead requires an outbox/saga before using this contract.
     * Payment processing and payment status belong entirely to Orders/Payments.
     */
    Receipt place(Request request);
}
