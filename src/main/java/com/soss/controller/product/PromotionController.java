package com.soss.controller.product;

import com.soss.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory/promotions")
public class PromotionController {
    private final PromotionService service;
    public PromotionController(PromotionService service) { this.service = service; }
    @GetMapping
    String list(Model model) {
        model.addAttribute("promotions", service.list());
        model.addAttribute("today", service.today());
        return "products/promotions";
    }
    @GetMapping("/new")
    String create(Model model) {
        PromotionForm form = new PromotionForm();
        form.setStartsOn(service.today()); form.setEndsOn(service.today().plusDays(7));
        model.addAttribute("promotionForm", form);
        return prepare(null, model);
    }
    @GetMapping("/{id}/edit")
    String edit(@PathVariable Long id, Model model) {
        model.addAttribute("promotionForm", service.form(id)); return prepare(id, model);
    }
    @PostMapping({"", "/{id}"})
    String save(@PathVariable(required = false) Long id, @Valid @ModelAttribute("promotionForm") PromotionForm form,
        BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try { service.save(id, form); }
            catch (IllegalArgumentException | OptimisticLockingFailureException e) { errors.reject("promotion", e.getMessage()); }
        }
        if (errors.hasErrors()) return prepare(id, model);
        redirect.addFlashAttribute("message", "Promotion saved.");
        return "redirect:/inventory/promotions";
    }
    private String prepare(Long id, Model model) {
        model.addAttribute("promotionId", id); model.addAttribute("products", service.products());
        return "products/promotion-form";
    }
}
