package com.sgs.backend.plateforme.dto;

import com.sgs.backend.roles.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Charge utile de POST /api/plateforme/entreprises : l'entreprise à créer
 * ET son premier compte ADMIN en une seule requête.
 *
 * Le rôle est volontairement ABSENT du DTO : un onboarding crée par
 * définition un ADMIN d'entreprise. Laisser le client choisir le rôle
 * permettrait de créer un SUPER_ADMIN via l'API publique -- jamais.
 */
public record AdminEntrepriseRequestDTO(
        @NotBlank(message = "le nom de l'entreprise est obligatoire")
        String nomEntreprise,

        String adresse1,
        String adresse2,
        String ville,
        String codePostal,
        String pays,

        String mailEntreprise,
        String numTelEntreprise,

        @NotBlank(message = "le prénom de l'admin est obligatoire")
        String adminPrenom,

        @NotBlank(message = "le nom de l'admin est obligatoire")
        String adminNom,

        @NotBlank(message = "le login de l'admin est obligatoire")
        String adminLogin,

        @NotBlank(message = "le mot de passe de l'admin est obligatoire")
        String adminMotDePasse,

        @Email(message = "l'adresse mail de l'admin doit être valide")
        String adminMail,

        String adminNumTel
) {
    /** Role fixé par le serveur, jamais par le client. */
    public static final UserRole ROLE_IMPOSE = UserRole.ADMIN;
}
