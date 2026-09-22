package com.sgs.backend.utilisateur.dto;

import com.sgs.backend.roles.UserRole;

// Jamais de motDePasse ici, même hashé -- il n'a rien à faire dans une
// réponse API (même principe que LoginResponse.UserInfo).
public record UtilisateurResponseDTO(
        Long id,
        String nom,
        String prenom,
        String login,
        String mail,
        String numTel,
        UserRole role
) {}
