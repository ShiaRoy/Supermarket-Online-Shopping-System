package com.soss.controller.accounts;
import com.soss.model.accounts.AddressForm;
import com.soss.service.accounts.AccountEnhancementService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile/addresses")
// Handles adding, editing, selecting, and removing addresses for the logged-in account.
public class AddressController {
    private final AccountEnhancementService service;
    public AddressController(AccountEnhancementService service) { this.service = service; }
    private void list(Principal principal, Model model) { model.addAttribute("addresses", service.addresses(principal.getName())); }
    @GetMapping public String index(Principal principal, Model model) {
        list(principal,model); model.addAttribute("addressForm",new AddressForm()); return "auth/addresses";
    }
    @GetMapping("/{id}/edit") public String edit(Principal principal, @PathVariable Long id, Model model) {
        model.addAttribute("addressForm", service.addressForm(principal.getName(),id)); model.addAttribute("editingId",id);
        list(principal,model); return "auth/addresses";
    }
    @PostMapping public String add(Principal principal,@Valid @ModelAttribute("addressForm") AddressForm form,
                                  BindingResult errors,Model model) { return save(principal,null,form,errors,model); }
    @PostMapping("/{id}") public String edit(Principal principal,@PathVariable Long id,
                    @Valid @ModelAttribute("addressForm") AddressForm form,BindingResult errors,Model model) {
        service.addressForm(principal.getName(),id); model.addAttribute("editingId",id);
        return save(principal,id,form,errors,model);
    }
    private String save(Principal principal,Long id,AddressForm form,BindingResult errors,Model model) {
        if (!errors.hasErrors()) {
            try { service.saveAddress(principal.getName(),id,form); return "redirect:/profile/addresses?saved"; }
            catch (IllegalArgumentException e) { errors.reject("limit",e.getMessage()); }
        }
        list(principal,model); return "auth/addresses";
    }
    @PostMapping("/{id}/default") public String use(Principal principal,@PathVariable Long id) {
        service.useAddress(principal.getName(),id); return "redirect:/profile/addresses?selected";
    }
    @PostMapping("/{id}/delete") public String delete(Principal principal,@PathVariable Long id) {
        service.deleteAddress(principal.getName(),id); return "redirect:/profile/addresses?deleted";
    }
}
