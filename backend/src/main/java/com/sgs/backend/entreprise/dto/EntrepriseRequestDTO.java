package com.sgs.backend.entreprise.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Champs d'adresse à plat, même choix que ClientRequestDTO : plus simple à
// mapper depuis un formulaire frontend qu'un sous-objet imbriqué.
public record EntrepriseRequestDTO(
        @NotBlank(message = "le nom de l'entreprise est obligatoire")
        String nom,

        String adresse1,
        String adresse2,
        String ville,
        String codePostal,
        String pays,

        @Email(message = "l'adresse mail doit être valide")
        String mail,

        String numTel
) {}
