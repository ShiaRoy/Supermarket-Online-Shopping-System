package com.soss.checkout;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class CartService {
 private final CartRepository carts; private final ProductRepository products; private final OrderRepository orders;
 public CartService(CartRepository c,ProductRepository p,OrderRepository o){carts=c;products=p;orders=o;}
 public record Line(Long id,String name,String icon,int quantity,BigDecimal unitPrice,BigDecimal total) {}
 public record Basket(List<Line> lines,BigDecimal subtotal,BigDecimal delivery,BigDecimal discount,BigDecimal total,String promo,String token,int count) {}
 public static final List<String> PAYMENTS=List.of("M-PAiSA","MyCash","Visa / Mastercard");
 // Provision once when adding the user to the application. Never accept the owner from a request.
 @Transactional public void provision(String owner){if(carts.findByOwner(owner).isEmpty()) carts.save(new Cart(owner));}
 private Cart locked(String owner){return carts.lockByOwner(owner).orElseThrow(()->new IllegalArgumentException("Your basket is not provisioned. Contact support."));}
 public List<String> slots(){
  LocalDate today=LocalDate.now(ZoneId.of("Pacific/Fiji"));
  return List.of(today.plusDays(1)+" | 10am – 12pm",today.plusDays(1)+" | 2pm – 4pm",today.plusDays(2)+" | 2pm – 4pm");
 }
 @Transactional public Basket view(String owner){return calculate(locked(owner));}
 private Basket calculate(Cart c){
  List<Line> lines=new ArrayList<>();
  c.quantities.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{
   Product p=products.findById(e.getKey()).orElseThrow(()->new IllegalArgumentException("A product is no longer available."));

   lines.add(new Line(p.id,p.name,p.icon,e.getValue(),p.price,p.price.multiply(BigDecimal.valueOf(e.getValue()))));
  });
  BigDecimal subtotal=lines.stream().map(Line::total).reduce(new BigDecimal("0.00"),BigDecimal::add);
  BigDecimal delivery=lines.isEmpty()?new BigDecimal("0.00"):new BigDecimal("3.00");
  BigDecimal discount="BULA10".equals(c.promo)?subtotal.multiply(new BigDecimal("0.10")).setScale(2,RoundingMode.HALF_UP):new BigDecimal("0.00");
  return new Basket(List.copyOf(lines),subtotal,delivery,discount,subtotal.add(delivery).subtract(discount),c.promo,c.checkoutToken,lines.size());
 }
 @Transactional public void change(String owner,Long productId,int delta){
  if(delta!=1 && delta!=-1) throw new IllegalArgumentException("Invalid quantity change.");
  Cart c=locked(owner);
  Product p=products.findById(productId).orElseThrow(()->new IllegalArgumentException("Unknown product."));
  if(delta>0 && !p.active) throw new IllegalArgumentException("Product unavailable.");
  int quantity=c.quantities.getOrDefault(productId,0)+delta;
  if(quantity<0 || quantity>99) throw new IllegalArgumentException("Quantity must be between 0 and 99.");
  if(quantity==0)c.quantities.remove(productId);else c.quantities.put(productId,quantity);
  c.checkoutToken=UUID.randomUUID().toString();
 }
 @Transactional public void promo(String owner,String code){
  Cart c=locked(owner);
  String normalized=code==null?"":code.strip().toUpperCase(Locale.ROOT);
  if(!normalized.isEmpty() && !"BULA10".equals(normalized)) throw new IllegalArgumentException("Promo code is not valid.");
  c.promo=normalized.isEmpty()?null:normalized; c.checkoutToken=UUID.randomUUID().toString();
 }
 @Transactional public ShopOrder place(String owner,CheckoutForm form){
  Cart c=locked(owner); // Serialize checkout with every cart mutation and duplicate submission.
  Optional<ShopOrder> previous=orders.findByOwnerAndCheckoutToken(owner,form.getToken());
  if(previous.isPresent()) return previous.get();
  if(!c.checkoutToken.equals(form.getToken())) throw new IllegalArgumentException("Your basket changed. Review it and place the order again.");
  if(form.getAddress()==null || form.getAddress().isBlank() || form.getAddress().length()>300) throw new IllegalArgumentException("Enter a delivery address (maximum 300 characters).");
  if(!slots().contains(form.getDeliverySlot())) throw new IllegalArgumentException("Choose an available delivery time.");
  if(!PAYMENTS.contains(form.getPaymentMethod())) throw new IllegalArgumentException("Choose a payment method.");
  for(Long id:c.quantities.keySet()) if(!products.findById(id).orElseThrow().active) throw new IllegalArgumentException("A product is unavailable. Remove it from your basket before checkout.");
  Basket b=calculate(c);
  if(b.lines().isEmpty()) throw new IllegalArgumentException("Your basket is empty.");
  ShopOrder o=new ShopOrder(); o.owner=owner; o.checkoutToken=c.checkoutToken;
  o.address=form.getAddress().strip(); o.deliverySlot=form.getDeliverySlot(); o.paymentMethod=form.getPaymentMethod();
  o.subtotal=b.subtotal(); o.delivery=b.delivery(); o.discount=b.discount(); o.total=b.total(); o.promo=c.promo;
  b.lines().forEach(l->o.items.add(new OrderItem(l.id(),l.name(),l.quantity(),l.unitPrice())));
  orders.saveAndFlush(o);
  c.quantities.clear();c.promo=null;c.checkoutToken=UUID.randomUUID().toString();
  return o;
 }
}

