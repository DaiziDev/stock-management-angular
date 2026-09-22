package com.sgs.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Config OpenAPI/Swagger — ajoute le bouton "Authorize" dans Swagger UI
 * pour que tu puisses entrer ton JWT et tester les endpoints protégés.
 *
 * Sans cette config, Swagger ne peut pas envoyer de header Authorization
 * et tous les endpoints protégés renverraient 403.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // Nom du schéma de sécurité pour le JWT
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("SGS — API Gestion de Stock")
                        .version("1.0")
                        .description("API REST du système de gestion de stock multi-entreprises SGS")
                        .contact(new Contact()
                                .name("SGS Team")
                                .email("dev@sgs.local")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Colle ton JWT ici après t'être connecté via POST /api/auth/login. " +
                                                "Format : colle directement le token (sans le préfixe 'Bearer ')."
                                        )));
    }
}
