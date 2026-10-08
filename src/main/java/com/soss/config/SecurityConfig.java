package com.soss.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .authorizeHttpRequests(auth -> auth

                        // Public pages
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/access-denied",
                                "/products",
                                "/products/**",
                                "/search",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/error"
                        ).permitAll()

                        // Admin
                        .requestMatchers(
                                "/admin",
                                "/admin/**"
                        ).hasRole("ADMIN")

                        // Staff inventory
                        .requestMatchers(
                                "/staff",
                                "/staff/**",
                                "/inventory",
                                "/inventory/**"
                        ).hasAnyRole("STAFF", "ADMIN")

                        // Customers can track their deliveries
                        .requestMatchers(
                                "/delivery/track",
                                "/delivery/track/**"
                        ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN", "DELIVERY")

                        // Staff/admin create deliveries and assign drivers
                        .requestMatchers(
                                HttpMethod.POST,
                                "/delivery/create",
                                "/delivery/*/driver"
                        ).hasAnyRole("STAFF", "ADMIN")

                        // Delivery staff may update delivery status
                        .requestMatchers(
                                HttpMethod.POST,
                                "/delivery/*/status"
                        ).hasAnyRole("STAFF", "ADMIN", "DELIVERY")

                        // Delivery management page
                        .requestMatchers(
                                "/delivery/manage"
                        ).hasAnyRole("STAFF", "ADMIN", "DELIVERY")

                        // Any other delivery route is staff-side
                        .requestMatchers(
                                "/delivery/**"
                        ).hasAnyRole("STAFF", "ADMIN", "DELIVERY")

                        // Customer shopping/order routes
                        .requestMatchers(
                                "/customer",
                                "/customer/**",
                                "/api/cart",
                                "/api/cart/**",
                                "/api/checkout",
                                "/cart",
                                "/cart/**",
                                "/checkout",
                                "/checkout/**",
                                "/orders",
                                "/orders/**",
                                "/payment-history",
                                "/payments",
                                "/payments/**"
                        ).hasRole("CUSTOMER")

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/profile", true)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                )

                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/access-denied")
                )

                .build();
    }
}
