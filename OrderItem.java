package com.soss.checkout;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Embeddable
public class OrderItem {
 @Column(nullable=false) public Long productId;
 @Column(nullable=false,length=120) public String name;
 @Column(nullable=false) public int quantity;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal unitPrice;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal lineTotal;
 protected OrderItem() {}
 public OrderItem(Long id,String name,int quantity,BigDecimal price) {
  this.productId=id; this.name=name; this.quantity=quantity; this.unitPrice=price; this.lineTotal=price.multiply(BigDecimal.valueOf(quantity));
 }
}

