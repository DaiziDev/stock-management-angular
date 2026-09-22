package com.sgs.backend.client.dto;

public record ClientResponseDTO(
        Long id,
        String nom,
        String prenom,
        String adresse1,
        String adresse2,
        String ville,
        String codePostal,
        String pays,
        String mail,
        String numTel,
        String photo
) {}
