package com.soss.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CheckoutForm {
    @Size(max = 255) private String address;
    private String deliveryArea;
    private String street;
    private String buildingNumber;
    private String deliveryInstructions;
    private Long savedAddressId;
    @NotBlank private String deliverySlot;
    @NotBlank private String paymentMethod;
    @NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private String token;

    public String getAddress() { return address; }
    public void setAddress(String value) { address = value; }
    public String getDeliveryArea() { return deliveryArea; }
    public void setDeliveryArea(String value) { deliveryArea = value; }
    public String getStreet() { return street; }
    public void setStreet(String value) { street = value; }
    public String getBuildingNumber() { return buildingNumber; }
    public void setBuildingNumber(String value) { buildingNumber = value; }
    public String getDeliveryInstructions() { return deliveryInstructions; }
    public void setDeliveryInstructions(String value) { deliveryInstructions = value; }
    public Long getSavedAddressId() { return savedAddressId; }
    public void setSavedAddressId(Long value) { savedAddressId = value; }
    public String getDeliverySlot() { return deliverySlot; }
    public void setDeliverySlot(String value) { deliverySlot = value; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String value) { paymentMethod = value; }
    public String getToken() { return token; }
    public void setToken(String value) { token = value; }
}