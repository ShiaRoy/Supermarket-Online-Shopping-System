package com.soss.model;
import jakarta.validation.constraints.*;

//validate and hold the details entered in the profile.
// Validates editable profile details without exposing email or role changes.
public class ProfileForm {
 @NotBlank @Size(max=100) private String fullName;
 @NotBlank @Pattern(regexp="[0-9]{7}",message="Enter seven digits after +679.") private String phone;
 @Size(max=500) private String deliveryAddress;
 @Pattern(regexp="|Suva Central|Nausori|Lautoka|Nadi",message="Choose a listed store.") private String preferredStore;
 public String getPreferredStore(){return preferredStore;} public void setPreferredStore(String v){preferredStore=v==null?null:v.trim();}
 public String getFullName(){return fullName;} public void setFullName(String v){fullName=v==null?null:v.trim();}
 public String getPhone(){return phone;} public void setPhone(String v){phone=v==null?null:v.trim();}
 public String getDeliveryAddress(){return deliveryAddress;} public void setDeliveryAddress(String v){deliveryAddress=v==null?null:v.trim();}
}
