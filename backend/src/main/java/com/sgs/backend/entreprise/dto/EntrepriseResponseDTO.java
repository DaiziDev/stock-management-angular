package com.sgs.backend.entreprise.dto;

public record EntrepriseResponseDTO(
        Long id,
        String nom,
        String adresse1,
        String adresse2,
        String ville,
        String codePostal,
        String pays,
        String mail,
        String numTel,

        // Nombre de comptes rattachés à l'entreprise -- affiché sur la
        // console plateforme. Compté par EntrepriseService (lazy, évite le
        // chargement de la collection côté entité).
        long nbUtilisateurs
) {}
