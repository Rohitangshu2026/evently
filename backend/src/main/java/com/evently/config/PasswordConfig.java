package com.evently.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the application's password hashing strategy.
 */
@Configuration
public class PasswordConfig {

    /**
     * Argon2id password encoder using Spring Security's current recommended
     * parameters. Argon2id is a memory-hard KDF resistant to GPU/ASIC cracking;
     * the underlying implementation is provided by BouncyCastle.
     *
     * @return the Argon2id-based {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
