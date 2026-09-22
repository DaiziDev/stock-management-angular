package com.sgs.backend.fournisseur.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FournisseurRequestDTO(
        @NotBlank(message = "le nom est obligatoire")
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
