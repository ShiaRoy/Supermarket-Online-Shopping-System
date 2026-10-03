package com.soss.controller.delivery;

import com.soss.model.delivery.Delivery;
import com.soss.model.delivery.DeliveryStatus;
import com.soss.service.delivery.DeliveryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    /** Order lookup: a customer types an order number and is taken to its tracking page. */
    @GetMapping("/track")
    public String lookup(@RequestParam(required = false) Long orderId, Model model) {
        if (orderId == null) {
            return "delivery/lookup";
        }
        if (deliveryService.findByOrderId(orderId).isEmpty()) {
            model.addAttribute("error", "No delivery found for order #" + orderId);
            return "delivery/lookup";
        }
        return "redirect:/delivery/track/" + orderId;
    }

    /** Customer view: "Track your grocery delivery" for one order. */
    @GetMapping("/track/{orderId}")
    public String track(@PathVariable Long orderId, Model model) {
        Delivery delivery = deliveryService.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No delivery found for this order"));
        model.addAttribute("delivery", delivery);
        model.addAttribute("steps", DeliveryStatus.values());
        return "delivery/track";
    }

    /** Staff view: all deliveries (optionally filtered by stage) with buttons to update them. */
    @GetMapping("/manage")
    public String manage(@RequestParam(required = false) DeliveryStatus status, Model model) {
        model.addAttribute("deliveries", deliveryService.findAll(status));
        model.addAttribute("statuses", DeliveryStatus.values());
        model.addAttribute("selected", status);
        return "delivery/manage";
    }

    /** Staff form: create a delivery for an order (the Orders module will call the same service method). */
    @PostMapping("/create")
    public String create(@RequestParam Long orderId,
                         @RequestParam String deliveryAddress,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expectedFrom,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expectedTo,
                         RedirectAttributes redirectAttributes) {
        try {
            deliveryService.createForOrder(orderId, deliveryAddress.trim(), expectedFrom, expectedTo);
            redirectAttributes.addFlashAttribute("message", "Delivery created for order #" + orderId + ".");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/delivery/manage";
    }

    /** Staff form: assign delivery staff by ID; leave it blank to remove the assignment. */
    @PostMapping("/{id}/driver")
    public String assignDriver(@PathVariable Long id,
                               @RequestParam(required = false) Long staffId,
                               RedirectAttributes redirectAttributes) {
        try {
            deliveryService.assignStaff(id, staffId);
            redirectAttributes.addFlashAttribute("message", "Driver updated.");
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return "redirect:/delivery/manage";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam DeliveryStatus status,
                               RedirectAttributes redirectAttributes) {
        try {
            deliveryService.advanceStatus(id, status);
            redirectAttributes.addFlashAttribute("message", "Delivery status updated.");
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/delivery/manage";
    }
}
