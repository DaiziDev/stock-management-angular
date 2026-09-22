package com.sgs.backend.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Champs d'adresse à plat, même choix que EntrepriseRequestDTO : plus simple
// à mapper depuis un formulaire frontend qu'un sous-objet imbriqué. Le
// Service reconstruit l'Adresse (@Embeddable) à partir de ces champs.
public record ClientRequestDTO(
        @NotBlank(message = "le nom est obligatoire")
        String nom,

        @NotBlank(message = "le prénom est obligatoire")
        String prenom,

        String adresse1,
        String adresse2,
        String ville,
        String codePostal,
        String pays,

        @Email(message = "l'adresse mail doit être valide")
        String mail,

        String numTel,

        String photo
) {}
