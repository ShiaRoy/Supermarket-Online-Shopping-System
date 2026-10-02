package com.soss.checkout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CartApiController {
    private final CartService carts;
    public CartApiController(CartService carts) { this.carts = carts; }
    public record QuantityRequest(@NotNull @Positive Long productId, @NotNull @Min(-1) @Max(1) Integer delta) {}
    public record PromoRequest(@NotNull @Size(max=32) String code) {}

    private String owner(Principal user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required.");
        return user.getName();
    }

    @GetMapping("/cart")
    public CartService.Basket cart(Principal user) { return carts.view(owner(user)); }

    @PostMapping("/cart/quantity")
    public CartService.Basket quantity(Principal user, @Valid @RequestBody QuantityRequest request) {
        String owner = owner(user);
        carts.change(owner, request.productId(), request.delta());
        return carts.view(owner);
    }

    @PostMapping("/cart/promo")
    public CartService.Basket promo(Principal user, @Valid @RequestBody PromoRequest request) {
        String owner = owner(user); carts.promo(owner, request.code()); return carts.view(owner);
    }

    @GetMapping("/cart/delivery-slots")
    public List<String> slots(Principal user) { owner(user); return carts.slots(); }

    @PostMapping("/checkout")
    public CheckoutOrders.Receipt checkout(Principal user, @Valid @RequestBody CheckoutForm form) {
        return carts.place(owner(user), form);
    }
}
