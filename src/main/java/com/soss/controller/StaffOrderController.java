package com.soss.controller;

import com.soss.service.StaffOrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff/orders")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class StaffOrderController {
    private final StaffOrderService orders;

    public StaffOrderController(StaffOrderService orders) { this.orders = orders; }

    @GetMapping
    public String queue(Model model) {
        model.addAttribute("orders", orders.queue());
        return "staff/orders";
    }

    @PostMapping("/{id}/processing")
    public String processing(@PathVariable Long id, RedirectAttributes flash) {
        try {
            orders.markProcessing(id);
            flash.addFlashAttribute("message", "Order #" + id + " moved to processing.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/staff/orders";
    }
}
