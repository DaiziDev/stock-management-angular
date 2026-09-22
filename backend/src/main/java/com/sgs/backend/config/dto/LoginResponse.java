package com.sgs.backend.config.dto;

import com.sgs.backend.roles.UserRole;

/**
 * DTO de réponse retourné après une connexion réussie.
 *
 * Contient :
 * - Le token JWT à stocker côté frontend (localStorage)
 * - Les infos utilisateur (sans le mot de passe) pour l'affichage
 *
 * entrepriseNom est fourni pour l'affichage dans la navbar -- le tenant
 * n'a pas besoin d'appeler GET /api/entreprises (réservé au SUPER_ADMIN)
 * juste pour connaître son propre nom.
 */
public record LoginResponse(
        String token,
        UserInfo user
) {
    /**
     * Informations utilisateur minimales renvoyées au frontend.
     * Pas de mot de passe, pas de données sensibles.
     * entrepriseId/entrepriseNom sont null pour le SUPER_ADMIN (plateforme).
     */
    public record UserInfo(
            Long id,
            String nom,
            String prenom,
            String login,
            UserRole role,
            Long entrepriseId,
            String entrepriseNom
    ) {}
}
