package com.soss.model;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

//check the registration details, including the email, password match and password length. 
public class RegistrationForm extends ProfileForm {
 @NotBlank @Email @Size(max=254) private String email;
 @NotBlank @Size(min=8,max=72,message="Use 8 to 72 characters.") private String password;
 @NotBlank private String confirmPassword;
 public String getEmail(){return email;} public void setEmail(String v){email=v==null?null:v.trim().toLowerCase(Locale.ROOT);}
 public String getPassword(){return password;} public void setPassword(String v){password=v;}
 public String getConfirmPassword(){return confirmPassword;} public void setConfirmPassword(String v){confirmPassword=v;}
 @AssertTrue(message="Passwords must match.") public boolean isPasswordsMatch(){return password!=null && password.equals(confirmPassword);}
 @AssertTrue(message="Password must fit within 72 UTF-8 bytes.") public boolean isPasswordWithinLimit(){return password==null || password.getBytes(StandardCharsets.UTF_8).length<=72;}
}
