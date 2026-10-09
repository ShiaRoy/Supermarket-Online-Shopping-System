package com.soss.model;
import jakarta.validation.constraints.*;
public class CheckoutForm {
 @NotBlank @Size(max=255) private String address;
 @NotBlank private String deliverySlot;
 @NotBlank private String paymentMethod;
 @NotBlank @Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") private String token;
 public String getAddress(){return address;} public void setAddress(String v){address=v;}
 public String getDeliverySlot(){return deliverySlot;} public void setDeliverySlot(String v){deliverySlot=v;}
 public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;}
 public String getToken(){return token;} public void setToken(String v){token=v;}
}

