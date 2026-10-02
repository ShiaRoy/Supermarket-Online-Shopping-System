package com.soss.checkout;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
@Controller
public class ShopController {
 private final CartService service; private final ProductRepository products;private final OrderRepository orders;
 public ShopController(CartService s,ProductRepository p,OrderRepository o){service=s;products=p;orders=o;}
 @GetMapping({"/","/search"}) String home(@RequestParam(defaultValue="") String q,Principal user,Model m){
  m.addAttribute("products",products.findByActiveTrueOrderByIdAsc().stream().filter(p->p.name.toLowerCase(java.util.Locale.ROOT).contains(q.toLowerCase(java.util.Locale.ROOT))).toList());
  m.addAttribute("q",q);m.addAttribute("basket",service.view(user.getName()));return "home";
 }
 private void cartModel(Principal user,Model m){
  m.addAttribute("basket",service.view(user.getName()));m.addAttribute("slots",service.slots());m.addAttribute("payments",CartService.PAYMENTS);
 }
 @GetMapping("/cart") String cart(Principal user,Model m){
  cartModel(user,m);CheckoutForm f=new CheckoutForm();f.setToken(((CartService.Basket)m.getAttribute("basket")).token());f.setPaymentMethod("M-PAiSA");
  m.addAttribute("checkout",f);return "cart";
 }
 @PostMapping("/cart/quantity") String quantity(Principal user,@RequestParam Long productId,@RequestParam int delta,RedirectAttributes flash){
  try{service.change(user.getName(),productId,delta);}catch(IllegalArgumentException e){flash.addFlashAttribute("error",e.getMessage());}return "redirect:/cart";
 }
 @PostMapping("/cart/promo") String promo(Principal user,@RequestParam String code,RedirectAttributes flash){
  try{service.promo(user.getName(),code);}catch(IllegalArgumentException e){flash.addFlashAttribute("error",e.getMessage());}return "redirect:/cart";
 }
 @PostMapping("/checkout") String checkout(Principal user,@Valid @ModelAttribute("checkout") CheckoutForm form,BindingResult errors,Model m){
  if(!errors.hasErrors()){
   try{return "redirect:/confirmation/"+service.place(user.getName(),form).id;}
   catch(IllegalArgumentException e){m.addAttribute("error",e.getMessage());}
  }
  cartModel(user,m);form.setToken(((CartService.Basket)m.getAttribute("basket")).token());return "cart";
 }
 @GetMapping("/confirmation/{id}") String confirmation(Principal user,@PathVariable Long id,Model m){
  m.addAttribute("order",orders.findByIdAndOwner(id,user.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)));
  m.addAttribute("basket",service.view(user.getName()));return "confirmation";
 }
 @GetMapping("/confirmation") String latest(Principal user){
  var mine=orders.findByOwnerOrderByCreatedAtDesc(user.getName());return mine.isEmpty()?"redirect:/orders":"redirect:/confirmation/"+mine.getFirst().id;
 }
 @GetMapping("/orders") String orders(Principal user,Model m){
  m.addAttribute("orders",orders.findByOwnerOrderByCreatedAtDesc(user.getName()));m.addAttribute("basket",service.view(user.getName()));return "orders";
 }
}

