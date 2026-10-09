package com.soss.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
// Defines which pages each role can access and configures login, logout, and session handling.
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    // Applies role restrictions and configures the authentication and session filters.
    SecurityFilterChain securityFilterChain(HttpSecurity http, org.springframework.security.core.session.SessionRegistry sessions) throws Exception {

        return http
                .authorizeHttpRequests(auth -> auth

                        // Public pages
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/forgot-password",
                                "/reset-password",
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

                        .requestMatchers("/support/manage", "/support/manage/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/support", "/support/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session.maximumSessions(-1)
                        .sessionRegistry(sessions).expiredUrl("/login?passwordChanged"))

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
