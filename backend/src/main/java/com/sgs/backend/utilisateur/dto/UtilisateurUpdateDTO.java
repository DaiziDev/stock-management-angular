package com.sgs.backend.utilisateur.dto;

import com.sgs.backend.roles.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Ni login ni motDePasse ici : renommer l'identifiant de connexion ou
// changer un mot de passe sont des opérations sensibles qui méritent leur
// propre endpoint dédié (et une ré-authentification), pas un simple champ
// dans un formulaire d'édition de profil.
public record UtilisateurUpdateDTO(
        @NotBlank(message = "le nom est obligatoire")
        String nom,

        @NotBlank(message = "le prénom est obligatoire")
        String prenom,

        @Email(message = "l'adresse mail doit être valide")
        String mail,

        String numTel,

        @NotNull(message = "le rôle est obligatoire")
        UserRole role
) {}
