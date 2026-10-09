package com.soss.controller.accounts;

import com.soss.model.accounts.*;
import com.soss.service.accounts.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import org.slf4j.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
// Handles password changes and email reset requests, with validation and session expiry.
public class PasswordController {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordController.class);
    private final PasswordService passwords;
    private final AccountSessions sessions;
    public PasswordController(PasswordService passwords, AccountSessions sessions) {
        this.passwords = passwords; this.sessions = sessions;
    }
    @GetMapping("/profile/password")
    public String change(Model model) { model.addAttribute("passwordForm", new PasswordForm()); return "auth/change-password"; }
    @PostMapping("/profile/password")
    public String change(Principal principal, @Valid @ModelAttribute("passwordForm") PasswordForm form,
                         BindingResult errors, HttpServletRequest request) {
        if (form.getCurrentPassword() == null || form.getCurrentPassword().isBlank())
            errors.rejectValue("currentPassword", "required", "Enter your current password.");
        if (errors.hasErrors()) return "auth/change-password";
        try { passwords.changePassword(principal.getName(), form); }
        catch (IllegalArgumentException e) { errors.reject("password",e.getMessage()); return "auth/change-password"; }
        sessions.expire(principal.getName()); request.getSession().invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return "redirect:/login?passwordChanged";
    }
    @GetMapping("/forgot-password")
    public String forgot(Model model) { model.addAttribute("forgotForm", new ForgotPasswordForm()); return "auth/forgot-password"; }
    @PostMapping("/forgot-password")
    public String forgot(@Valid @ModelAttribute("forgotForm") ForgotPasswordForm form, BindingResult errors) {
        if (errors.hasErrors()) return "auth/forgot-password";
        try { passwords.requestReset(form.getEmail()); }
        catch (RuntimeException e) {
            // Do not expose account existence, passwords, reset links, or SMTP credentials.
            LOG.warn("Password reset could not be delivered. Check database and SMTP configuration. Failure type: {}",e.getClass().getSimpleName());
        }
        return "redirect:/forgot-password?sent";
    }
    @GetMapping("/reset-password")
    public String reset(@RequestParam(defaultValue="") String token, Model model) {
        model.addAttribute("token", token); model.addAttribute("validToken", passwords.isValidToken(token));
        model.addAttribute("passwordForm",new PasswordForm()); return "auth/reset-password";
    }
    @PostMapping("/reset-password")
    public String reset(@RequestParam(defaultValue="") String token,
                        @Valid @ModelAttribute("passwordForm") PasswordForm form, BindingResult errors,
                        Model model, HttpServletRequest request) {
        model.addAttribute("token",token); model.addAttribute("validToken",passwords.isValidToken(token));
        if (errors.hasErrors()) return "auth/reset-password";
        try {
            String email = passwords.resetPassword(token,form); sessions.expire(email);
            // End this browser's session as well; other sessions expire on their next request.
            if (request.getSession(false) != null) request.getSession(false).invalidate();
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        } catch (IllegalArgumentException e) {
            errors.reject("token",e.getMessage()); return "auth/reset-password";
        }
        return "redirect:/login?passwordChanged";
    }
}
