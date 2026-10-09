package com.soss.controller.product;

import com.soss.model.Store;
import com.soss.service.CatalogueService;
import com.soss.service.PromotionService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CatalogueController {
    private final CatalogueService service;
    private final PromotionService promotions;

    public CatalogueController(CatalogueService service, PromotionService promotions) {
        this.service = service;
        this.promotions = promotions;
    }

    @GetMapping({"/products", "/search"})
    String catalogue(@RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            @RequestParam(defaultValue = "") String availability,
            @RequestParam(defaultValue = "name") String sort,
            Model model) {
        String selectedSort = switch (sort) {
            case "price-asc", "price-desc" -> sort;
            default -> "name";
        };
        model.addAttribute("sort", selectedSort);
        if (service.stores().isEmpty()) {
            model.addAttribute("stores", java.util.List.of());
            model.addAttribute("items", java.util.List.of());
            model.addAttribute("categories", java.util.List.of());
            model.addAttribute("matchingCount", 0);
            model.addAttribute("categoryCounts", Map.of());
            model.addAttribute("categoryGroups", Map.of());
            model.addAttribute("q", q);
            model.addAttribute("category", category);
            model.addAttribute("min", min);
            model.addAttribute("max", max);
            model.addAttribute("availability", availability);
            return "products/index";
        }
        Store store = service.store(storeId);
        model.addAttribute("store", store);
        model.addAttribute("stores", service.stores());
        var prices = promotions.activePrices();
        model.addAttribute("promotionPrices", prices);
        Comparator<com.soss.model.Inventory> byName = Comparator.comparing(
                item -> item.getProduct().getName(), String.CASE_INSENSITIVE_ORDER);
        Comparator<com.soss.model.Inventory> ordering = selectedSort.equals("name") ? byName
                : Comparator.comparing(item -> prices.getOrDefault(item.getProduct().getId(), item.getProduct().getPrice()));
        if (selectedSort.equals("price-desc")) ordering = ordering.reversed().thenComparing(byName);
        else if (selectedSort.equals("price-asc")) ordering = ordering.thenComparing(byName);
        var matchingItems = service.search(store.getId(), q, "", null, null, availability, false).stream()
                .filter(item -> min == null || prices.getOrDefault(item.getProduct().getId(), item.getProduct().getPrice()).compareTo(min) >= 0)
                .filter(item -> max == null || prices.getOrDefault(item.getProduct().getId(), item.getProduct().getPrice()).compareTo(max) <= 0)
                .toList();
        var items = matchingItems.stream()
                .filter(item -> category == null || category.isBlank() || item.getProduct().getCategory().equals(category))
                .sorted(ordering).toList();
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        for (String availableCategory : service.categories(store.getId(), false)) {
            categoryCounts.put(availableCategory, (int) matchingItems.stream()
                    .filter(item -> item.getProduct().getCategory().equals(availableCategory)).count());
        }
        Map<String, java.util.List<com.soss.model.Inventory>> categoryGroups = new LinkedHashMap<>();
        for (com.soss.model.Inventory item : items) {
            categoryGroups.computeIfAbsent(item.getProduct().getCategory(), key -> new java.util.ArrayList<>()).add(item);
        }
        model.addAttribute("items", items);
        model.addAttribute("matchingCount", matchingItems.size());
        model.addAttribute("categories", categoryCounts.keySet());
        model.addAttribute("categoryCounts", categoryCounts);
        model.addAttribute("categoryGroups", categoryGroups);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("min", min);
        model.addAttribute("max", max);
        model.addAttribute("availability", availability);
        return "products/index";
    }

    @GetMapping("/products/{id}")
    String detail(@PathVariable Long id, @RequestParam(required = false) Long storeId, Model model) {
        Store store = service.store(storeId);
        model.addAttribute("store", store);
        model.addAttribute("item", service.detail(id, store.getId(), false));
        model.addAttribute("promotionPrices", promotions.activePrices());
        return "products/product";
    }
}
