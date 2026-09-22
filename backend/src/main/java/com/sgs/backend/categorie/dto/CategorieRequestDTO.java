package com.sgs.backend.categorie.dto;

import jakarta.validation.constraints.NotBlank;

// DTO = Data Transfer Object. On ne reçoit JAMAIS directement l'entité JPA
// dans le corps d'une requête HTTP, pour 3 raisons concrètes :
// 1. Sécurité : le client ne doit pas pouvoir injecter n'importe quel champ
//    (ex: imposer un id, ou modifier createdAt/updatedAt).
// 2. Découplage : la forme de ta base de données peut changer sans casser
//    le contrat de l'API, et inversement.
// 3. Validation : on valide ici la forme des données ENTRANTES précisément,
//    indépendamment des contraintes de la table.
public record CategorieRequestDTO(
        @NotBlank(message = "le code est obligatoire")
        String code,

        @NotBlank(message = "la désignation est obligatoire")
        String designation
) {}
