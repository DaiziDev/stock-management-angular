package com.sgs.backend.config.dto;

import com.sgs.backend.roles.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de requête pour l'inscription d'un nouvel utilisateur.
 *
 * Réservé aux ADMIN uniquement (contrôle côté controller).
 * Le mot de passe sera hashé côté backend avant sauvegarde.
 */
public record RegisterRequest(
        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        String prenom,

        @NotBlank(message = "Le login est obligatoire")
        String login,

        @NotBlank(message = "Le mot de passe est obligatoire")
        String motDePasse,

        String mail,

        String numTel,

        @NotNull(message = "Le rôle est obligatoire")
        UserRole role,

        @NotNull(message = "L'entreprise est obligatoire")
        Long entrepriseId
) {}
