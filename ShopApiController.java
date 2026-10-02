package com.soss.checkout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ShopApiController {
    private final CartService carts;
    private final ProductRepository products;
    private final OrderRepository orders;

    public ShopApiController(CartService carts, ProductRepository products, OrderRepository orders) {
        this.carts = carts; this.products = products; this.orders = orders;
    }

    public record QuantityRequest(@NotNull @Positive Long productId,
                                  @NotNull @Min(-1) @Max(1) Integer delta) {}
    public record PromoRequest(@NotNull @Size(max=32) String code) {}
    public record ProductView(Long id, String name, String icon, BigDecimal price, String currency) {}
    public record ItemView(Long productId, String name, int quantity, BigDecimal unitPrice, BigDecimal total) {}
    public record OrderView(Long id, Instant createdAt, String address, String deliverySlot,
                            String paymentMethod, String paymentStatus, String currency,
                            BigDecimal subtotal, BigDecimal delivery, BigDecimal discount,
                            BigDecimal total, List<ItemView> items) {
        static OrderView of(ShopOrder o) {
            return new OrderView(o.id, o.createdAt, o.address, o.deliverySlot, o.paymentMethod,
                o.paymentStatus, "FJD", o.subtotal, o.delivery, o.discount, o.total,
                o.items.stream().map(i -> new ItemView(i.productId, i.name, i.quantity, i.unitPrice, i.lineTotal)).toList());
        }
    }
    public record CsrfView(String headerName, String token) {}

    @GetMapping("/csrf")
    public CsrfView csrf(CsrfToken token) {
        return new CsrfView(token.getHeaderName(), token.getToken());
    }

    @GetMapping("/products")
    public List<ProductView> products(@RequestParam(defaultValue="") String q) {
        String query = q.toLowerCase(java.util.Locale.ROOT);
        return products.findByActiveTrueOrderByIdAsc().stream()
            .filter(p -> p.name.toLowerCase(java.util.Locale.ROOT).contains(query))
            .map(p -> new ProductView(p.id, p.name, p.icon, p.price, "FJD")).toList();
    }

    @GetMapping("/cart")
    public CartService.Basket cart(Principal user) { return carts.view(user.getName()); }

    @PostMapping("/cart/quantity")
    public CartService.Basket quantity(Principal user, @Valid @RequestBody QuantityRequest request) {
        carts.change(user.getName(), request.productId(), request.delta());
        return carts.view(user.getName());
    }

    @PostMapping("/cart/promo")
    public CartService.Basket promo(Principal user, @Valid @RequestBody PromoRequest request) {
        carts.promo(user.getName(), request.code());
        return carts.view(user.getName());
    }

    @GetMapping("/delivery-slots")
    public List<String> slots() { return carts.slots(); }

    @PostMapping("/checkout")
    public OrderView checkout(Principal user, @Valid @RequestBody CheckoutForm form) {
        return OrderView.of(carts.place(user.getName(), form));
    }

    @GetMapping("/orders")
    public List<OrderView> orders(Principal user) {
        return orders.findByOwnerOrderByCreatedAtDesc(user.getName()).stream().map(OrderView::of).toList();
    }

    @GetMapping("/orders/{id}")
    public OrderView order(Principal user, @PathVariable Long id) {
        return OrderView.of(orders.findByIdAndOwner(id, user.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.")));
    }
}
