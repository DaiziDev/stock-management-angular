package com.sgs.backend.config.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requête pour la connexion.
 *
 * Le frontend envoie login + motDePasse.
 * Le backend vérifie et retourne un JWT si tout est OK.
 *
 * Pas de @Email ici : le champ "login" peut être un email OU un identifiant
 * simple (ex: "ravel.k"), pas forcément un email.
 */
public record LoginRequest(
        @NotBlank(message = "Le login est obligatoire")
        String login,

        @NotBlank(message = "Le mot de passe est obligatoire")
        String motDePasse
) {}
