package com.soss.service;

import com.soss.model.Cart;
import com.soss.model.CheckoutForm;
import com.soss.repository.CartRepository;
import com.soss.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class CartService {
    private final CartRepository carts;
    private final UserRepository users;
    private final CheckoutProducts products;
    private final CheckoutOrders orders;

    public CartService(CartRepository carts, CheckoutProducts products, CheckoutOrders orders, UserRepository users) {
        this.carts = carts; this.products = products; this.orders = orders; this.users = users;
    }

    public record Line(Long id, String name, String icon, int quantity, BigDecimal unitPrice, BigDecimal total) {}
    public record Basket(List<Line> lines, BigDecimal subtotal, BigDecimal delivery,
                         BigDecimal discount, BigDecimal total, String promo, String token, int count,
                         List<PromoCodes.PromoDiscount> appliedPromos) {}
    /** A historical order line supplied by the Orders module for a customer-requested reorder. */
    public record ReorderLine(Long productId, String productName, Integer quantity) {}
    /** The result distinguishes added lines from items skipped or adjusted against live stock. */
    public record ReorderResult(int addedQuantity, List<String> notices) {}
    // UI choices only; reconcile with the shared Payments module's enum at integration.
    public static final List<String> PAYMENTS = List.of("M-PAiSA", "MyCash", "Visa / Mastercard");

    /** Ensure an authenticated account has a cart; first visits also call this path. */
    @Transactional
    public void provision(String owner) {
        locked(owner);
    }

    private Cart locked(String owner) {
        if (owner == null || owner.isBlank() || owner.length() > 254) {
            throw new IllegalArgumentException("Invalid account identity.");
        }
        // The user row exists even before the cart, so first visits can be serialized.
        users.lockByEmail(owner).orElseThrow(
                () -> new IllegalArgumentException("Customer account not found."));
        return carts.lockByOwner(owner).orElseGet(() -> carts.saveAndFlush(new Cart(owner)));
    }

    public List<String> slots() {
        return new CartSlots().available();
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
            if (entry.getKey() == null || entry.getKey() <= 0 || entry.getValue() == null
                    || entry.getValue() < 1 || entry.getValue() > 99)
                throw new IllegalArgumentException("Quantity must be between 1 and 99.");
            var p = product(entry.getKey());
            BigDecimal price = p.price().setScale(2, RoundingMode.UNNECESSARY);
            lines.add(new Line(p.id(), p.name(), p.icon(), entry.getValue(),
                price, price.multiply(BigDecimal.valueOf(entry.getValue()))));
        });
        BigDecimal subtotal = lines.stream().map(Line::total).reduce(new BigDecimal("0.00"), BigDecimal::add);
        BigDecimal delivery = new BigDecimal(lines.isEmpty() ? "0.00" : "3.00");
        List<PromoCodes.PromoDiscount> appliedPromos = PromoCodes.breakdown(cart.promo, subtotal, delivery);
        BigDecimal discount = appliedPromos.stream().map(PromoCodes.PromoDiscount::amount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add)
                .min(subtotal.add(delivery)).max(BigDecimal.ZERO);
        BigDecimal total = subtotal.add(delivery).subtract(discount).max(BigDecimal.ZERO);
        return new Basket(List.copyOf(lines), subtotal, delivery, discount, total,
                cart.promo, cart.checkoutToken, lines.size(), appliedPromos);
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

    /**
     * Merges historical order lines into the customer's existing cart using current default-store
     * availability. This does not reserve or deduct stock; checkout remains responsible for that.
     */
    @Transactional
    public ReorderResult reorder(String owner, List<ReorderLine> orderLines) {
        Cart cart = locked(owner);
        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException("This order has no items to reorder.");
        }

        Map<Long, ReorderLine> requested = new LinkedHashMap<>();
        for (ReorderLine line : orderLines) {
            if (line == null || line.productId() == null || line.quantity() == null
                    || line.quantity() <= 0) {
                continue;
            }
            requested.merge(line.productId(), line,
                    (first, duplicate) -> new ReorderLine(first.productId(),
                            first.productName(), safeQuantityTotal(first.quantity(), duplicate.quantity())));
        }

        int added = 0;
        List<String> notices = new ArrayList<>();
        for (ReorderLine line : requested.values()) {
            String historicalName = line.productName() == null || line.productName().isBlank()
                    ? "This product" : line.productName();
            Optional<CheckoutProducts.ProductSnapshot> current = products.find(line.productId());
            if (current.isEmpty()) {
                notices.add(historicalName + " is no longer available.");
                continue;
            }

            CheckoutProducts.ProductSnapshot product = current.get();
            String name = product.name() == null || product.name().isBlank()
                    ? historicalName : product.name();
            if (!product.active()) {
                notices.add(name + (product.availableQuantity() <= 0
                        ? " is out of stock." : " is not currently available."));
                continue;
            }

            int currentQuantity = cart.quantities.getOrDefault(line.productId(), 0);
            int permittedQuantity = Math.min(99, Math.max(0, product.availableQuantity()));
            int requestedQuantity = Math.min(99, line.quantity());
            int finalQuantity = Math.min(permittedQuantity,
                    safeQuantityTotal(currentQuantity, requestedQuantity));
            int lineAdded = Math.max(0, finalQuantity - currentQuantity);
            if (lineAdded == 0) {
                notices.add(name + " is already at the available quantity in your cart.");
                continue;
            }

            cart.quantities.put(line.productId(), finalQuantity);
            added += lineAdded;
            if (lineAdded < line.quantity()) {
                notices.add(name + ": added " + lineAdded + " of " + line.quantity()
                        + " because only " + permittedQuantity + " are currently available.");
            }
        }
        if (added > 0) {
            cart.checkoutToken = UUID.randomUUID().toString();
        }
        return new ReorderResult(added, List.copyOf(notices));
    }

    private int safeQuantityTotal(int first, int second) {
        return first > Integer.MAX_VALUE - second ? Integer.MAX_VALUE : first + second;
    }

    @Transactional
    public void promo(String owner, String code) {
        Cart cart = locked(owner);
        String normalized = PromoCodes.normalize(code);
        if (normalized == null) throw new IllegalArgumentException("Enter a promo code.");
        Basket basket = calculate(cart);
        PromoCodes.requireEligible(normalized, basket.subtotal());
        cart.promo = PromoCodes.append(cart.promo, normalized);
        cart.checkoutToken = UUID.randomUUID().toString();
    }

    @Transactional
    public void removePromo(String owner, String code) {
        Cart cart = locked(owner);
        cart.promo = PromoCodes.remove(cart.promo, code);
        cart.checkoutToken = UUID.randomUUID().toString();
    }
    @Transactional
    public CheckoutOrders.Receipt place(String owner, CheckoutForm form) {
        if (form == null) throw new IllegalArgumentException("Checkout details are required.");
        if (form.getToken() == null || !form.getToken().matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw new IllegalArgumentException("A valid checkout token is required.");
        Cart cart = locked(owner);
        var existing = orders.findExisting(owner, form.getToken());
        if (existing.isPresent()) return existing.get();
        if (!cart.checkoutToken.equals(form.getToken()))
            throw new IllegalArgumentException("Your basket changed. Review it and place the order again.");
        boolean hasStructuredAddress = form.getDeliveryArea() != null || form.getStreet() != null
                || form.getBuildingNumber() != null || form.getDeliveryInstructions() != null;
        if (form.getSavedAddressId() == null && hasStructuredAddress) {
            form.setAddress(DeliveryAddressPolicy.compose(form.getDeliveryArea(), form.getStreet(),
                    form.getBuildingNumber(), form.getDeliveryInstructions()));
        }
        form.setAddress(DeliveryAddressPolicy.validate(form.getAddress()));
        if (form.getDeliverySlot() == null || !slots().contains(form.getDeliverySlot())) throw new IllegalArgumentException("Choose an available delivery time.");
        if (form.getPaymentMethod() == null || !PAYMENTS.contains(form.getPaymentMethod())) throw new IllegalArgumentException("Choose a payment method.");
        for (Long id : cart.quantities.keySet()) {
            if (!product(id).active()) throw new IllegalArgumentException("A product is unavailable. Remove it before checkout.");
        }
        Basket basket = calculate(cart);
        if (basket.lines().isEmpty()) throw new IllegalArgumentException("Your basket is empty.");
        PromoCodes.requireEligible(cart.promo, basket.subtotal());
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
