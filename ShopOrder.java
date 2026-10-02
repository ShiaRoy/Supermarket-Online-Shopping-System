package com.soss.checkout;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="shop_order",uniqueConstraints=@UniqueConstraint(columnNames={"owner","checkoutToken"}))
public class ShopOrder {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=100) public String owner;
 @Column(nullable=false,length=36) public String checkoutToken;
 @Column(nullable=false) public Instant createdAt=Instant.now();
 @Column(nullable=false,length=300) public String address;
 @Column(nullable=false,length=80) public String deliverySlot;
 @Column(nullable=false,length=30) public String paymentMethod;
 @Column(nullable=false,length=30) public String paymentStatus="PENDING";
 @Column(precision=12,scale=2,nullable=false) public BigDecimal subtotal;
 @Column(precision=12,scale=2,nullable=false) public BigDecimal delivery;
 @Column(precision=12,scale=2,nullable=false) public BigDecimal discount;
 @Column(precision=12,scale=2,nullable=false) public BigDecimal total;
 @Column(length=32) public String promo;
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="shop_order_item",joinColumns=@JoinColumn(name="order_id"))
 @OrderColumn(name="item_position") public List<OrderItem> items=new ArrayList<>();
}

