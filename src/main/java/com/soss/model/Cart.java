package com.soss.checkout;
import jakarta.persistence.*;
import java.util.*;
@Entity @Table(name="shop_cart")
public class Cart {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,unique=true,length=100) public String owner;
 @Column(length=32) public String promo;
 @Column(nullable=false,length=36) public String checkoutToken=UUID.randomUUID().toString();
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="shop_cart_line",joinColumns=@JoinColumn(name="cart_id"))
 @MapKeyColumn(name="product_id") @Column(name="quantity",nullable=false)
 public Map<Long,Integer> quantities=new HashMap<>();
 protected Cart() {}
 public Cart(String owner) { this.owner=owner; }
}

