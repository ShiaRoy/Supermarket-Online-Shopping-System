package com.soss.config;
import org.springframework.context.annotation.*;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
@Configuration
//tracks login sessions.
public class AccountSessionConfig {
    // tracks authenticated sessions.
    @Bean public SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }
    //notifies spring security.
    @Bean public HttpSessionEventPublisher httpSessionEventPublisher() { return new HttpSessionEventPublisher(); }
}
