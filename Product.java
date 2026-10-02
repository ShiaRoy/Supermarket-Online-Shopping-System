package com.soss.checkout;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="shop_product")
public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=120) public String name;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal price;
 @Column(nullable=false) public boolean active=true;
 @Column(nullable=false,length=12) public String icon;
 protected Product() {}
 public Product(String name,String price,String icon) { this.name=name; this.price=new BigDecimal(price); this.icon=icon; }
}

