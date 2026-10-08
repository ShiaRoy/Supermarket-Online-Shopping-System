package com.soss.model;
import jakarta.persistence.*;

//store user account details.
@Entity
@Table(name="users", uniqueConstraints=@UniqueConstraint(name="uk_users_email", columnNames="email"))
// Stores account details, the hashed password, role, and delivery preferences.
public class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=100) private String fullName;
 @Column(nullable=false,length=254) private String email;
 @Column(nullable=false,length=100) private String passwordHash;
 @Column(nullable=false,length=7) private String phone;
 @Column(length=500) private String deliveryAddress;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Role role;
 @Column(length=40) private String preferredStore;
 public String getPreferredStore(){return preferredStore;} public void setPreferredStore(String v){preferredStore=v;}
 public Long getId(){return id;}
 public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;}
 public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
 public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
 public String getDeliveryAddress(){return deliveryAddress;} public void setDeliveryAddress(String v){deliveryAddress=v;}
 public Role getRole(){return role;} public void setRole(Role v){role=v;}
}
