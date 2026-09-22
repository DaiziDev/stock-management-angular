package com.sgs.backend.categorie.dto;

// DTO de sortie : ce que l'API renvoie au client. On choisit précisément
// ce qu'on expose (ici pas besoin d'exposer createdAt/updatedAt côté liste
// des catégories par exemple, mais on pourrait les ajouter si besoin).
public record CategorieResponseDTO(
        Long id,
        String code,
        String designation
) {}
