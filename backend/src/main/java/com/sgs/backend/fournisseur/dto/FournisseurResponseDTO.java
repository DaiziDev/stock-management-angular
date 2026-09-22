package com.sgs.backend.fournisseur.dto;

public record FournisseurResponseDTO(
        Long id,
        String nom,
        String adresse1,
        String adresse2,
        String ville,
        String codePostal,
        String pays,
        String mail,
        String numTel
) {}
