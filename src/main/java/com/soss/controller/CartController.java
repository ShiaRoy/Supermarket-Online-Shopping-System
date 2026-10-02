package com.soss.controller;

import com.soss.model.CheckoutForm;
import com.soss.service.CartService;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;

@Controller
public class CartController {
    private final CartService carts;
    public CartController(CartService carts) { this.carts = carts; }

    private String owner(Principal user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return user.getName();
    }

    private void model(String owner, Model model) {
        model.addAttribute("basket", carts.view(owner));
        model.addAttribute("slots", carts.slots());
        model.addAttribute("payments", CartService.PAYMENTS);
    }

    @GetMapping("/cart")
    public String cart(Principal user, Model model) {
        model(owner(user), model);
        CheckoutForm form = new CheckoutForm();
        form.setToken(((CartService.Basket)model.getAttribute("basket")).token());
        form.setPaymentMethod("M-PAiSA");
        model.addAttribute("checkout", form);
        return "cart";
    }

    @PostMapping("/cart/quantity")
    public String quantity(Principal user, @RequestParam Long productId, @RequestParam int delta, RedirectAttributes flash) {
        try { carts.change(owner(user), productId, delta); }
        catch (IllegalArgumentException e) { flash.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/cart";
    }

    @PostMapping("/cart/promo")
    public String promo(Principal user, @RequestParam String code, RedirectAttributes flash) {
        try { carts.promo(owner(user), code); }
        catch (IllegalArgumentException e) { flash.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(Principal user, @Valid @ModelAttribute("checkout") CheckoutForm form,
                           BindingResult errors, Model model) {
        String owner = owner(user);
        if (!errors.hasErrors()) {
            try {
                // This destination is owned by the shared Orders module. Align its route during integration.
                return "redirect:/confirmation/" + carts.place(owner, form).orderId();
            } catch (IllegalArgumentException e) { model.addAttribute("error", e.getMessage()); }
        }
        model(owner, model);
        form.setToken(((CartService.Basket)model.getAttribute("basket")).token());
        return "cart";
    }
}
