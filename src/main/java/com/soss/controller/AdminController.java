package com.soss.controller;

import com.soss.model.Role;
import com.soss.service.AdminDashboardService;
import com.soss.service.AdminManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminDashboardService dashboard;
    private final AdminManagementService management;

    public AdminController(AdminDashboardService dashboard, AdminManagementService management) {
        this.dashboard = dashboard;
        this.management = management;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", dashboard.dashboard());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("accounts", management.accounts());
        model.addAttribute("assignableRoles", new Role[]{Role.CUSTOMER, Role.STAFF, Role.DELIVERY});
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam Role role, RedirectAttributes flash) {
        try {
            management.changeRole(id, role);
            flash.addFlashAttribute("message", "Account role updated.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("orders", dashboard.orders());
        return "admin/orders";
    }
}
