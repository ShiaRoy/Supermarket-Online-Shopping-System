package com.soss.controller.delivery;

import com.soss.model.delivery.Delivery;
import com.soss.model.delivery.DeliveryStatus;
import com.soss.model.Order;
import com.soss.model.Role;
import com.soss.model.User;
import com.soss.service.AccountService;
import com.soss.service.OrderService;
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
import java.security.Principal;
import java.util.Objects;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final OrderService orderService;
    private final AccountService accountService;

    public DeliveryController(
            DeliveryService deliveryService,
            OrderService orderService,
            AccountService accountService) {
        this.deliveryService = deliveryService;
        this.orderService = orderService;
        this.accountService = accountService;
    }

    /** Order lookup: a customer types an order number and is taken to its tracking page. */
    @GetMapping("/track")
    public String lookup(
            @RequestParam(required = false) Long orderId,
            Principal principal,
            Model model) {
        if (orderId == null) {
            return "delivery/lookup";
        }
        requireTrackingAccess(orderId, principal);
        if (deliveryService.findByOrderId(orderId).isEmpty()) {
            model.addAttribute("error", "Delivery tracking is not available for this order yet.");
            return "delivery/lookup";
        }
        return "redirect:/delivery/track/" + orderId;
    }

    /** Customer view: "Track your grocery delivery" for one order. */
    @GetMapping("/track/{orderId}")
    public String track(
            @PathVariable Long orderId,
            Principal principal,
            Model model) {
        requireTrackingAccess(orderId, principal);
        Delivery delivery = deliveryService.findByOrderId(orderId).orElse(null);
        if (delivery == null) {
            model.addAttribute("error", "Delivery tracking is not available for this order yet.");
            return "delivery/lookup";
        }
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

    private void requireTrackingAccess(Long orderId, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }

        User user = accountService.profile(principal.getName());
        if (user.getRole() != Role.CUSTOMER) {
            return;
        }

        Order order;
        try {
            order = orderService.getOrderById(orderId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        if (!Objects.equals(order.getCustomerId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your delivery.");
        }
    }
}
