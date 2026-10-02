package com.soss.controller.product;

import com.soss.controller.product.ProductForm;
import com.soss.model.Store;
import com.soss.service.CatalogueService;
import jakarta.validation.Valid;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class InventoryController {
    private final CatalogueService service;
    public InventoryController(CatalogueService service) { this.service = service; }
    @GetMapping
    String list(@RequestParam(required = false) Long storeId, @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "") String category, @RequestParam(defaultValue = "") String availability, Model model) {
        if (service.stores().isEmpty()) {
            model.addAttribute("stores", java.util.List.of());
            model.addAttribute("items", java.util.List.of());
            model.addAttribute("categories", java.util.List.of());
            return "products/inventory";
        }
        Store store = service.store(storeId);
        model.addAttribute("store", store); model.addAttribute("stores", service.stores());
        model.addAttribute("items", service.search(store.getId(), q, category, null, null, availability, true));
        model.addAttribute("categories", service.categories(store.getId(), true));
        model.addAttribute("q", q); model.addAttribute("category", category); model.addAttribute("availability", availability);
        return "products/inventory";
    }
    @PostMapping("/stores")
    String addStore(@RequestParam String name, RedirectAttributes redirect) {
        if (name.isBlank() || name.strip().length() > 100) {
            redirect.addFlashAttribute("error", "Enter a store name of 1 to 100 characters.");
            return "redirect:/inventory";
        }
        Store store = service.addStore(name.strip());
        redirect.addFlashAttribute("message", "Store added.");
        return "redirect:/inventory?storeId=" + store.getId();
    }
    @GetMapping("/new")
    String create(@RequestParam(required = false) Long storeId, Model model) {
        ProductForm form = new ProductForm();
        form.setStoreId(service.store(storeId).getId());
        model.addAttribute("productForm", form);
        return prepareForm(null, form, model);
    }
    @GetMapping("/{id}/edit")
    String edit(@PathVariable Long id, @RequestParam(required = false) Long storeId, Model model) {
        ProductForm form = service.editForm(id, service.store(storeId).getId());
        model.addAttribute("productForm", form);
        return prepareForm(id, form, model);
    }
    @PostMapping({"", "/{id}"})
    String save(@PathVariable(required = false) Long id, @Valid @ModelAttribute("productForm") ProductForm form,
        BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try { service.save(id, form); }
            catch (OptimisticLockingFailureException exception) {
                errors.reject("conflict", "Another update was saved. Reload this page before saving again.");
            }
        }
        if (errors.hasErrors()) return prepareForm(id, form, model);
        redirect.addFlashAttribute("message", id == null ? "Product added." : "Product and stock updated.");
        return "redirect:/inventory?storeId=" + form.getStoreId();
    }
    private String prepareForm(Long id, ProductForm form, Model model) {
        model.addAttribute("productId", id);
        model.addAttribute("store", service.store(form.getStoreId()));
        return "products/product-form";
    }
}
