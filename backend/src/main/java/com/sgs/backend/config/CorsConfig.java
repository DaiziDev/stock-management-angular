package com.sgs.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration CORS — consommée par SPRING SECURITY (via http.cors() dans
 * SecurityConfig), pas par Spring MVC.
 *
 * Pourquoi ce niveau et pas WebMvcConfigurer : la chaîne de filtres Spring
 * Security s'exécute AVANT le DispatcherServlet. Sans CORS côté security,
 * la requête OPTIONS (preflight) -- qui ne porte jamais de header
 * Authorization -- est rejetée 401 avant même d'atteindre le CORS MVC :
 * le navigateur affiche "No 'Access-Control-Allow-Origin' header" pour
 * tous les endpoints protégés (symptôme observé sur /api/auth/me,
 * /api/entreprises...), alors que /api/auth/login (permitAll) passait.
 *
 * Le CorsFilter de Security gère alors preflight + headers sur TOUTES les
 * requêtes, avant la règle anyRequest().authenticated().
 */
@Configuration
public class CorsConfig {

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Origine exacte du serveur de dev Angular (requis avec allowCredentials).
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Le JWT part en header, pas en cookie : credentials inutiles.
        config.setAllowCredentials(false);
        // Cache du preflight : évite un OPTIONS avant chaque requête.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
