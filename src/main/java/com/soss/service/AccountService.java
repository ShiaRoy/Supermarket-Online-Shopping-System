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
//handle account registration, passowrd hashing, profile updates and loading user details for login.
@Service
public class AccountService implements UserDetailsService {
 private final UserRepository users; private final PasswordEncoder encoder;
 public AccountService(UserRepository users,PasswordEncoder encoder){this.users=users;this.encoder=encoder;}
 @Transactional public void register(RegistrationForm f){
  if(users.existsByEmail(f.getEmail())) throw new IllegalArgumentException("An account already uses that email.");
  User u=new User();u.setFullName(f.getFullName());u.setEmail(f.getEmail());u.setPhone(f.getPhone());u.setPasswordHash(encoder.encode(f.getPassword()));u.setRole(Role.CUSTOMER);users.saveAndFlush(u);
 }
 @Transactional(readOnly=true) public User profile(String email){return users.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("Account unavailable"));}
 @Transactional public void update(String email,ProfileForm f){User u=profile(email);u.setFullName(f.getFullName());u.setPhone(f.getPhone());u.setDeliveryAddress(f.getDeliveryAddress());}
 @Override @Transactional(readOnly=true) public UserDetails loadUserByUsername(String email){User u=profile(email.trim().toLowerCase(Locale.ROOT));return org.springframework.security.core.userdetails.User.withUsername(u.getEmail()).password(u.getPasswordHash()).roles(u.getRole().name()).build();}
}
