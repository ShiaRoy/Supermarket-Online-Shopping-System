package com.soss.controller.product;

import com.soss.model.*;
import com.soss.service.CatalogueService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@Controller
public class CatalogueController {
    private final CatalogueService service;
    public CatalogueController(CatalogueService service) { this.service = service; }
    @GetMapping({"/products", "/search"})
    String catalogue(@RequestParam(required = false) Long storeId,
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "") String category,
        @RequestParam(required = false) BigDecimal min,
        @RequestParam(required = false) BigDecimal max,
        @RequestParam(defaultValue = "") String availability,
        jakarta.servlet.http.HttpServletRequest request, Model model) {
        if (service.stores().isEmpty()) {
            model.addAttribute("stores", java.util.List.of());
            model.addAttribute("items", java.util.List.of());
            model.addAttribute("categories", java.util.List.of());
            model.addAttribute("q", q);
            return "products/index";
        }
        Store store = service.store(storeId);
        model.addAttribute("store", store); model.addAttribute("stores", service.stores());
        model.addAttribute("items", service.search(store.getId(), q, category, min, max, availability, false));
        model.addAttribute("categories", service.categories(store.getId(), false));
        model.addAttribute("q", q); model.addAttribute("category", category);
        model.addAttribute("min", min); model.addAttribute("max", max);
        model.addAttribute("availability", availability);
        return request.getServletPath().equals("/products") ? "products/index" : "products/search";
    }
    @GetMapping("/products/{id}")
    String detail(@PathVariable Long id, @RequestParam(required = false) Long storeId, Model model) {
        Store store = service.store(storeId);
        model.addAttribute("store", store);
        model.addAttribute("item", service.detail(id, store.getId(), false));
        return "products/product";
    }
}
