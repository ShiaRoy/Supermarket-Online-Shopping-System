package com.soss.controller;
import com.soss.model.*;
import com.soss.service.AccountService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

//handle requests for the registration, login and user profiles.
@Controller
// Handles registration, login pages, profile editing, and role-specific access pages.
public class AccountController {
 private final AccountService accounts;
 
 //controller is connected to the account service.
 public AccountController(AccountService accounts){this.accounts=accounts;}
 @GetMapping("/login") public String login(){return "auth/login";}
 @GetMapping("/register") public String registration(Model m){m.addAttribute("registration",new RegistrationForm());return "auth/register";}
 @PostMapping("/register") public String register(@Valid @ModelAttribute("registration") RegistrationForm f,BindingResult errors){
  if(errors.hasErrors())return "auth/register";
  try{accounts.register(f);}catch(IllegalArgumentException|DataIntegrityViolationException e){errors.rejectValue("email","duplicate","An account already uses that email.");return "auth/register";}
  return "redirect:/login?registered";
 }
 @GetMapping("/profile") public String profile(Principal p,Model m){User u=accounts.profile(p.getName());ProfileForm f=new ProfileForm();f.setFullName(u.getFullName());f.setPhone(u.getPhone());f.setDeliveryAddress(u.getDeliveryAddress());f.setPreferredStore(u.getPreferredStore());m.addAttribute("profile",f);m.addAttribute("account",u);return "auth/profile";}
 @PostMapping("/profile") public String save(Principal p,@Valid @ModelAttribute("profile") ProfileForm f,BindingResult errors,Model m){
  if(errors.hasErrors()){m.addAttribute("account",accounts.profile(p.getName()));return "auth/profile";}
  accounts.update(p.getName(),f);return "redirect:/profile?saved";
 }
 
 //display the customer access page.
@GetMapping("/customer")
public String customerArea(Model m) {
    m.addAttribute("area", "customer");
    return "auth/access";
}
//display the staff access page.
@GetMapping("/staff")
public String staffArea() {
    return "redirect:/inventory";
}
//display the admin access page.
@GetMapping("/admin")
public String adminArea() {
    return "redirect:/admin/dashboard";
}

 @GetMapping("/access-denied")
 @ResponseStatus(HttpStatus.FORBIDDEN)
 public String accessDenied(){return "auth/access-denied";}
}
