package com.soss.controller.accounts;
import com.soss.model.accounts.*;
import com.soss.service.accounts.AccountEnhancementService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
@Controller
// Lets customers submit queries and lets staff or admins reply and update their status.
public class SupportController {
    private final AccountEnhancementService service;
    public SupportController(AccountEnhancementService service) { this.service=service; }
    @GetMapping("/support") public String customer(Principal principal,@RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("supportForm",new SupportForm()); model.addAttribute("queries",service.myQueries(principal.getName(),page));
        return "auth/support";
    }
    @PostMapping("/support") public String send(Principal principal,@Valid @ModelAttribute("supportForm") SupportForm form,
                                             BindingResult errors,Model model) {
        if(errors.hasErrors()) { model.addAttribute("queries",service.myQueries(principal.getName(),0)); return "auth/support"; }
        service.createQuery(principal.getName(),form); return "redirect:/support?sent";
    }
    @GetMapping("/support/manage") public String manage(@RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("queries",service.allQueries(page)); model.addAttribute("replyForm",new SupportReplyForm());
        model.addAttribute("statuses",SupportStatus.values()); return "auth/support-manage";
    }
    // Stores the support reply, status, and update time.
    @PostMapping("/support/manage/{id}") public String reply(@PathVariable Long id,
            @Valid @ModelAttribute("replyForm") SupportReplyForm form,BindingResult errors,Model model) {
        if(errors.hasErrors()) { model.addAttribute("queries",service.allQueries(0)); model.addAttribute("statuses",SupportStatus.values()); return "auth/support-manage"; }
        service.reply(id,form); return "redirect:/support/manage?saved";
    }
}
