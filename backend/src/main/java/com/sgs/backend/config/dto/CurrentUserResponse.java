package com.sgs.backend.config.dto;

import com.sgs.backend.roles.UserRole;

/**
 * DTO de réponse retourné par GET /api/auth/me.
 *
 * Permet au frontend de récupérer les informations de l'utilisateur
 * connecté à partir de son token JWT (sans re-demander login/mot de passe).
 *
 * entrepriseNom évite au tenant d'appeler GET /api/entreprises (réservé au
 * SUPER_ADMIN) juste pour afficher son nom dans la navbar.
 * entrepriseId/entrepriseNom sont null pour le SUPER_ADMIN (plateforme).
 */
public record CurrentUserResponse(
        Long id,
        String nom,
        String prenom,
        String login,
        String mail,
        String numTel,
        UserRole role,
        Long entrepriseId,
        String entrepriseNom
) {}
