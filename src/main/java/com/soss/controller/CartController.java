package com.soss.controller;

import com.soss.model.CheckoutForm;
import com.soss.service.AccountService;
import com.soss.service.CartService;
import com.soss.service.DeliveryAddressPolicy;
import com.soss.service.accounts.AccountEnhancementService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
// Displays cart and checkout pages and processes customer shopping forms.
public class CartController {
    private final CartService carts;
    private final AccountService accounts;
    private final AccountEnhancementService accountEnhancements;

    public CartController(CartService carts, AccountService accounts,
            AccountEnhancementService accountEnhancements) {
        this.carts = carts;
        this.accounts = accounts;
        this.accountEnhancements = accountEnhancements;
    }

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
        model.addAttribute("deliveryAreas", DeliveryAddressPolicy.AREAS);
        model.addAttribute("savedAddresses", accountEnhancements.addresses(owner));
        model.addAttribute("profileAddress", accounts.profile(owner).getDeliveryAddress());
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
        form.setToken(((CartService.Basket) model.getAttribute("basket")).token());
        form.setPaymentMethod("M-PAiSA");
        String profileAddress = (String) model.getAttribute("profileAddress");
        if (profileAddress != null && !profileAddress.isBlank()) {
            form.setSavedAddressId(-1L);
            form.setAddress(profileAddress);
        }
        for (var saved : accountEnhancements.addresses(owner)) {
            if (saved.getDefaultAddress()) {
                form.setSavedAddressId(saved.getId());
                form.setAddress(saved.getAddress());
                break;
            }
        }
        model.addAttribute("checkout", form);
        return "checkout";
    }

    @PostMapping("/cart/quantity")
    public String quantity(Principal user, @RequestParam Long productId, @RequestParam int delta,
            @RequestParam(required = false) String returnTo, RedirectAttributes flash) {
        try {
            carts.change(owner(user), productId, delta);
            if (delta > 0) flash.addFlashAttribute("message", "Item added to your cart.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + shoppingReturnTo(returnTo);
    }

    @PostMapping("/cart/promo")
    public String promo(Principal user, @RequestParam String code, RedirectAttributes flash) {
        try {
            carts.promo(owner(user), code);
            flash.addFlashAttribute("message", code.strip().toUpperCase() + " applied.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/promo/remove")
    public String removePromo(Principal user, @RequestParam String code, RedirectAttributes flash) {
        try {
            carts.removePromo(owner(user), code);
            flash.addFlashAttribute("message", code + " removed.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(Principal user, @Valid @ModelAttribute("checkout") CheckoutForm form,
            BindingResult errors, Model model) {
        String owner = owner(user);
        if (!errors.hasErrors()) {
            try {
                if (form.getSavedAddressId() != null) {
                    Long id = form.getSavedAddressId();
                    if (id == -1L) {
                        form.setAddress(accounts.profile(owner).getDeliveryAddress());
                    } else if (id > 0) {
                        form.setAddress(accountEnhancements.addressForCheckout(owner, id));
                    } else {
                        throw new IllegalArgumentException("Choose a valid saved address or enter a new one.");
                    }
                }
                return "redirect:/orders/" + carts.place(owner, form).orderId() + "/confirmation";
            } catch (IllegalArgumentException | IllegalStateException exception) {
                model.addAttribute("error", exception.getMessage());
            } catch (ResponseStatusException exception) {
                model.addAttribute("error", exception.getReason() == null
                        ? "Checkout is temporarily unavailable. Please try again shortly."
                        : exception.getReason());
            }
        }
        checkoutModel(owner, model);
        form.setToken(((CartService.Basket) model.getAttribute("basket")).token());
        model.addAttribute("checkout", form);
        return "checkout";
    }

    private String shoppingReturnTo(String returnTo) {
        if (returnTo != null && returnTo.startsWith("/products") && !returnTo.startsWith("//")) {
            return returnTo;
        }
        return "/cart";
    }
}