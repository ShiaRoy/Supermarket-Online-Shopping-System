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

    private void basketModel(String owner, Model model) {
        model.addAttribute("basket", carts.view(owner));
    }

    private void checkoutModel(String owner, Model model) {
        basketModel(owner, model);
        model.addAttribute("slots", carts.slots());
        model.addAttribute("payments", CartService.PAYMENTS);
    }

    @GetMapping("/cart")
    public String cart(Principal user, Model model) {
        basketModel(owner(user), model);
        return "cart";
    }

    @GetMapping("/checkout")
    public String checkoutPage(Principal user, Model model) {
        String owner = owner(user);
        checkoutModel(owner, model);
        CheckoutForm form = new CheckoutForm();
        form.setToken(((CartService.Basket)model.getAttribute("basket")).token());
        form.setPaymentMethod("M-PAiSA");
        model.addAttribute("checkout", form);
        return "checkout";
    }

    @PostMapping("/cart/quantity")
    public String quantity(Principal user, @RequestParam Long productId, @RequestParam int delta,
                           @RequestParam(required = false) String returnTo, RedirectAttributes flash) {
        try {
            carts.change(owner(user), productId, delta);
            if (delta > 0) {
                flash.addFlashAttribute("message", "Item added to your cart.");
            }
        }
        catch (IllegalArgumentException e) { flash.addFlashAttribute("error", e.getMessage()); }
        return "redirect:" + shoppingReturnTo(returnTo);
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
                // Use the existing Orders confirmation page.
                return "redirect:/orders/" + carts.place(owner, form).orderId() + "/confirmation";
            } catch (IllegalArgumentException e) { model.addAttribute("error", e.getMessage()); }
        }
        checkoutModel(owner, model);
        form.setToken(((CartService.Basket)model.getAttribute("basket")).token());
        return "checkout";
    }

    private String shoppingReturnTo(String returnTo) {
        if (returnTo != null && returnTo.startsWith("/products") && !returnTo.startsWith("//")) {
            return returnTo;
        }
        return "/cart";
    }
}
