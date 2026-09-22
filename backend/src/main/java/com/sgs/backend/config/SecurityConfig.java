package com.sgs.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration Spring Security.
 *
 * Principe clé : STATELESS (pas de session côté serveur).
 * Chaque requête doit contenir le JWT dans le header Authorization.
 * Le serveur ne garde AUCUNE trace de connexion entre les requêtes.
 *
 * Le PasswordEncoder est défini dans PasswordConfig pour éviter
 * la dépendance circulaire SecurityConfig → JwtAuthFilter → UtilisateurService
 * → PasswordEncoder → SecurityConfig.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UrlBasedCorsConfigurationSource corsConfigurationSource;

    /**
     * AuthenticationManager est requis par Spring Security pour
     * l'authentification classique (username/password).
     * On l'expose ici pour pouvoir l'utiliser dans AuthController.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Chaîne de filtres de sécurité — le cœur de la config.
     *
     * L'ordre des filtres compte :
     * 1. CorsConfig (déjà défini séparément)
     * 2. Désactivation du CSRF (inutile en API stateless)
     * 3. Pas de session (STATELESS)
     * 4. Règles d'autorisation sur les endpoints
     * 5. NotreJwtFilter AVANT UsernamePasswordAuthenticationFilter
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Désactiver CSRF — inutile pour une API REST stateless
            // Le CSRF protège contre les formulaires HTML volant des tokens de session
            // Comme on utilise JWT (pas de cookie de session), pas besoin
            .csrf(AbstractHttpConfigurer::disable)

            // Activer le CORS AU NIVEAU SECURITY, en amont de l'autorisation :
            // le preflight OPTIONS (sans header Authorization) est sinon rejeté
            // 401 avant d'atteindre le CORS MVC -- "No 'Access-Control-Allow-Origin'
            // header" sur tous les endpoints protégés depuis le navigateur.
            // Le bean CorsConfigurationSource est défini dans CorsConfig.
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // Configurer les règles d'accès aux endpoints
            .authorizeHttpRequests(auth -> auth
                // ── Endpoints PUBLICS (pas besoin d'être connecté) ──
                // Seul /login est public : /register et /me lisent
                // @AuthenticationPrincipal et plantent (NPE -> 500) si appelés
                // sans JWT -- ils doivent passer par la règle "authenticated"
                // ci-dessous pour recevoir un 401/403 propre à la place.
                .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/refresh",      // refresh : le JWT est expiré, forcément anonyme
                        "/api/auth/logout",       // logout : révocable même après expiration du JWT
                        "/swagger-ui.html",       // Swagger UI
                        "/swagger-ui/**",         // Swagger UI ressources
                        "/api-docs/**",           // OpenAPI docs
                        "/v3/api-docs/**"         // OpenAPI docs v3
                ).permitAll()

                // ── Tout le reste nécessite une authentification ──
                .anyRequest().authenticated()
            )

            // Pas de session côté serveur — chaque requête est indépendante
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Ajouter notre filtre JWT AVANT le filtre standard Spring Security
            // Cela permet d'authentifier les requêtes avec notre token
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
