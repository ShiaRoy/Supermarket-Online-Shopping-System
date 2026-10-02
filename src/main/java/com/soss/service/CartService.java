package com.soss.checkout;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class CartService {
    private final CartRepository carts;
    private final CheckoutProducts products;
    private final CheckoutOrders orders;

    public CartService(CartRepository carts, CheckoutProducts products, CheckoutOrders orders) {
        this.carts = carts; this.products = products; this.orders = orders;
    }

    public record Line(Long id, String name, String icon, int quantity, BigDecimal unitPrice, BigDecimal total) {}
    public record Basket(List<Line> lines, BigDecimal subtotal, BigDecimal delivery,
                         BigDecimal discount, BigDecimal total, String promo, String token, int count) {}
    // UI choices only; reconcile with the shared Payments module's enum at integration.
    public static final List<String> PAYMENTS = List.of("M-PAiSA", "MyCash", "Visa / Mastercard");

    /** Call during account creation, or migrate existing accounts before enabling cart routes. */
    @Transactional
    public void provision(String owner) {
        if (owner == null || owner.isBlank() || owner.length() > 100)
            throw new IllegalArgumentException("Invalid account identity.");
        if (carts.findByOwner(owner).isEmpty()) carts.save(new Cart(owner));
    }

    private Cart locked(String owner) {
        return carts.lockByOwner(owner).orElseThrow(
            () -> new IllegalArgumentException("Your basket is not provisioned. Contact support."));
    }

    public List<String> slots() {
        LocalDate today = LocalDate.now(ZoneId.of("Pacific/Fiji"));
        return List.of(today.plusDays(1) + " | 10am – 12pm",
                       today.plusDays(1) + " | 2pm – 4pm",
                       today.plusDays(2) + " | 2pm – 4pm");
    }

    @Transactional
    public Basket view(String owner) { return calculate(locked(owner)); }

    private CheckoutProducts.ProductSnapshot product(Long id) {
        var p = products.find(id).orElseThrow(() -> new IllegalArgumentException("A product is no longer available."));
        if (!id.equals(p.id()) || p.price() == null || p.price().signum() < 0)
            throw new IllegalArgumentException("Invalid product information.");
        return p;
    }

    private Basket calculate(Cart cart) {
        List<Line> lines = new ArrayList<>();
        cart.quantities.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            var p = product(entry.getKey());
            BigDecimal price = p.price().setScale(2, RoundingMode.UNNECESSARY);
            lines.add(new Line(p.id(), p.name(), p.icon(), entry.getValue(),
                price, price.multiply(BigDecimal.valueOf(entry.getValue()))));
        });
        BigDecimal subtotal = lines.stream().map(Line::total).reduce(new BigDecimal("0.00"), BigDecimal::add);
        BigDecimal delivery = new BigDecimal(lines.isEmpty() ? "0.00" : "3.00");
        BigDecimal discount = "BULA10".equals(cart.promo)
            ? subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP)
            : new BigDecimal("0.00");
        return new Basket(List.copyOf(lines), subtotal, delivery, discount, subtotal.add(delivery).subtract(discount),
            cart.promo, cart.checkoutToken, lines.size());
    }

    @Transactional
    public void change(String owner, Long productId, int delta) {
        if (productId == null || productId <= 0 || (delta != 1 && delta != -1))
            throw new IllegalArgumentException("Invalid quantity change.");
        Cart cart = locked(owner);
        // Existing unavailable/deleted items may still be decremented and removed.
        if (delta > 0 && !product(productId).active()) throw new IllegalArgumentException("Product unavailable.");
        int quantity = cart.quantities.getOrDefault(productId, 0) + delta;
        if (quantity < 0 || quantity > 99) throw new IllegalArgumentException("Quantity must be between 0 and 99.");
        if (quantity == 0) cart.quantities.remove(productId); else cart.quantities.put(productId, quantity);
        cart.checkoutToken = UUID.randomUUID().toString();
    }

    @Transactional
    public void promo(String owner, String code) {
        Cart cart = locked(owner);
        String normalized = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
        if (!normalized.isEmpty() && !"BULA10".equals(normalized))
            throw new IllegalArgumentException("Promo code is not valid.");
        cart.promo = normalized.isEmpty() ? null : normalized;
        cart.checkoutToken = UUID.randomUUID().toString();
    }

    @Transactional
    public CheckoutOrders.Receipt place(String owner, CheckoutForm form) {
        Cart cart = locked(owner);
        var existing = orders.findExisting(owner, form.getToken());
        if (existing.isPresent()) return existing.get();
        if (!cart.checkoutToken.equals(form.getToken()))
            throw new IllegalArgumentException("Your basket changed. Review it and place the order again.");
        if (form.getAddress() == null || form.getAddress().isBlank() || form.getAddress().length() > 300)
            throw new IllegalArgumentException("Enter a delivery address (maximum 300 characters).");
        if (!slots().contains(form.getDeliverySlot())) throw new IllegalArgumentException("Choose an available delivery time.");
        if (!PAYMENTS.contains(form.getPaymentMethod())) throw new IllegalArgumentException("Choose a payment method.");
        for (Long id : cart.quantities.keySet()) {
            if (!product(id).active()) throw new IllegalArgumentException("A product is unavailable. Remove it before checkout.");
        }
        Basket basket = calculate(cart);
        if (basket.lines().isEmpty()) throw new IllegalArgumentException("Your basket is empty.");
        var items = basket.lines().stream().map(line -> new CheckoutOrders.Item(
            line.id(), line.name(), line.quantity(), line.unitPrice(), line.total())).toList();
        var request = new CheckoutOrders.Request(owner, cart.checkoutToken, form.getAddress().strip(),
            form.getDeliverySlot(), form.getPaymentMethod(), "FJD", basket.subtotal(), basket.delivery(),
            basket.discount(), basket.total(), cart.promo, items);
        var receipt = orders.place(request);
        if (receipt == null || receipt.orderId() == null)
            throw new IllegalStateException("The shared Orders module did not return an order ID.");
        cart.quantities.clear(); cart.promo = null; cart.checkoutToken = UUID.randomUUID().toString();
        return receipt;
    }
}
