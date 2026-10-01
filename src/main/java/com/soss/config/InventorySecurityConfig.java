package com.soss.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class InventorySecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain inventorySecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher("/inventory", "/inventory/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/inventory/login").permitAll()
                .anyRequest().hasAnyRole("STAFF", "ADMIN"))
            .formLogin(login -> login.loginPage("/inventory/login")
                .loginProcessingUrl("/inventory/login").defaultSuccessUrl("/inventory", false)
                .failureUrl("/inventory/login?error"))
            .logout(logout -> logout.logoutUrl("/inventory/logout").logoutSuccessUrl("/products"))
            .build();
    }

    // Optional local login until the team's User authentication module is integrated.
    @Bean
    @ConditionalOnProperty(name = "inventory.local-login.enabled", havingValue = "true")
    UserDetailsService localInventoryUser(@Value("${inventory.local-login.password}") String password) {
        if (password.length() < 12) throw new IllegalArgumentException("Local staff password must be at least 12 characters.");
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new InMemoryUserDetailsManager(User.withUsername("divit")
            .password(encoder.encode(password)).roles("STAFF").build());
    }
}
