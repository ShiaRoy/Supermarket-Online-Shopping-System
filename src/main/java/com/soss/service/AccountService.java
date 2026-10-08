package com.soss.service;
import com.soss.model.*;
import com.soss.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
//handle accounts registration, password hashing, profile updates and loading user details. 
@Service
// Registers customers, updates profiles, and supplies account details for login.
public class AccountService implements UserDetailsService {
 private final UserRepository users; private final PasswordEncoder encoder;
 private final com.soss.repository.accounts.SavedAddressRepository addresses;
 public AccountService(UserRepository users,PasswordEncoder encoder,com.soss.repository.accounts.SavedAddressRepository addresses){this.users=users;this.encoder=encoder;this.addresses=addresses;}
 
 @Transactional public void register(RegistrationForm f){
  if(users.existsByEmail(f.getEmail())) throw new IllegalArgumentException("An account already uses that email.");
  User u=new User();u.setFullName(f.getFullName());u.setEmail(f.getEmail());u.setPhone(f.getPhone());u.setPasswordHash(encoder.encode(f.getPassword()));u.setRole(Role.CUSTOMER);users.saveAndFlush(u);
 }
 @Transactional(readOnly=true) public User profile(String email){return users.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("Account unavailable"));}

 @Transactional public void update(String email,ProfileForm f){User u=users.lockByEmail(email).orElseThrow(()->new UsernameNotFoundException("Account unavailable"));u.setFullName(f.getFullName());u.setPhone(f.getPhone());u.setPreferredStore(f.getPreferredStore());u.setDeliveryAddress(f.getDeliveryAddress());
 
  for(var a:addresses.findByOwnerIdOrderByIdAsc(u.getId())) if(a.getDefaultAddress() && !java.util.Objects.equals(a.getAddress(),f.getDeliveryAddress())) a.setDefaultAddress(false);}
 @Override @Transactional(readOnly=true) public UserDetails loadUserByUsername(String email){User u=profile(email.trim().toLowerCase(Locale.ROOT));return org.springframework.security.core.userdetails.User.withUsername(u.getEmail()).password(u.getPasswordHash()).roles(u.getRole().name()).build();}
}
