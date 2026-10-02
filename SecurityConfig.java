package com.soss.checkout;

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SecurityConfig {
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a -> a.requestMatchers("/css/**", "/error").permitAll()
                .anyRequest().hasRole("CUSTOMER"))
            // Same authenticated session as the HTML pages. CSRF remains enabled on every API mutation.
            .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), new AntPathRequestMatcher("/api/**")))
            .formLogin(f -> f.defaultSuccessUrl("/cart", true))
            .logout(l -> l.logoutSuccessUrl("/login?logout")).build();
    }

    // Standalone demonstration only. Replace with the existing SOSS account service when integrating.
    @Bean @Profile("demo")
    UserDetailsService demoUser(@Value("${SOSS_DEMO_PASSWORD}") String password) {
        if (password.length() < 12) throw new IllegalArgumentException("Set SOSS_DEMO_PASSWORD to at least 12 characters.");
        return new InMemoryUserDetailsManager(User.withUsername("vaibhav")
            .password(new BCryptPasswordEncoder().encode(password)).roles("CUSTOMER").build());
    }

    @Bean org.springframework.security.crypto.password.PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }
}
