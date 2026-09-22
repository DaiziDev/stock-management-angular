package com.sgs.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bean PasswordEncoder isolé pour casser la dépendance circulaire :
 *
 *   Avant : SecurityConfig → PasswordEncoder
 *           SecurityConfig → JwtAuthFilter → UtilisateurService → PasswordEncoder (dans SecurityConfig) 
 *
 *   Après : PasswordConfig → PasswordEncoder (indépendant)
 *           SecurityConfig → PasswordEncoder (de PasswordConfig) 
 *           JwtAuthFilter → UtilisateurService → PasswordEncoder (de PasswordConfig) 
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
