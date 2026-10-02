package com.soss.config;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// set up login, logout and role access for application.
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(a->a
   .requestMatchers("/", "/login", "/register", "/products", "/products/**", "/search", "/css/**", "/js/**", "/images/**", "/error").permitAll()
   .requestMatchers("/admin","/admin/**").hasRole("ADMIN")
   .requestMatchers("/staff","/staff/**","/inventory","/inventory/**").hasAnyRole("STAFF","ADMIN")
   .requestMatchers("/customer","/customer/**","/cart","/cart/**","/checkout","/checkout/**","/orders","/orders/**").hasRole("CUSTOMER")
   .anyRequest().authenticated())
    // use email to log in and open the profile after successful login.
   .formLogin(f->f.loginPage("/login").usernameParameter("email").defaultSuccessUrl("/profile",true).permitAll())
   
    // return to the login page after logout.
   .logout(l->l.logoutSuccessUrl("/login?logout")).build();
 }
}
