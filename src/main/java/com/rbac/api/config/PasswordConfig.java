package com.rbac.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Registers a single PasswordEncoder bean so every component
 * (UserService, DataInitializer) hashes and verifies passwords the same way.
 */
@Configuration
public class PasswordConfig {

    /** BCrypt adds a random salt per password and is deliberately slow, which resists brute force. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
