package com.soss.model;
import jakarta.validation.constraints.*;

//validate and hold the details entered in the profile.
public class ProfileForm {
 @NotBlank @Size(max=100) private String fullName;
 @NotBlank @Pattern(regexp="[0-9]{7}",message="Enter seven digits after +679.") private String phone;
 @Size(max=500) private String deliveryAddress;
 public String getFullName(){return fullName;} public void setFullName(String v){fullName=v==null?null:v.trim();}
 public String getPhone(){return phone;} public void setPhone(String v){phone=v==null?null:v.trim();}
 public String getDeliveryAddress(){return deliveryAddress;} public void setDeliveryAddress(String v){deliveryAddress=v==null?null:v.trim();}
}
