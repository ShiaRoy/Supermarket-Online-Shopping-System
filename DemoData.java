package com.soss.checkout;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;
import org.springframework.transaction.annotation.Transactional;
@Component @Profile("demo")
public class DemoData implements CommandLineRunner {
 private final ProductRepository products;private final CartRepository carts;
 public DemoData(ProductRepository p,CartRepository c){products=p;carts=c;}
 @Override @Transactional public void run(String... args){
  if(products.count()==0){
   products.save(new Product("Fresh Milk 2L","4.50","🥛"));
   products.save(new Product("White Bread","3.20","🍞"));
   products.save(new Product("Chicken 1kg","12.99","🍗"));
  }
  if(carts.findByOwner("vaibhav").isEmpty()){
   Cart c=new Cart("vaibhav"); var list=products.findByActiveTrueOrderByIdAsc();
   for(int i=0;i<list.size();i++)c.quantities.put(list.get(i).id,i==0?2:1);
   c.promo="BULA10";carts.save(c);
  }
 }
}

