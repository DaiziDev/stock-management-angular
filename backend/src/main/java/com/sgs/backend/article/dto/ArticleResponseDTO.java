package com.sgs.backend.article.dto;

import java.math.BigDecimal;

public record ArticleResponseDTO(
        Long id,
        String codeArticle,
        String designation,
        BigDecimal prixUnitaireHt,
        BigDecimal tauxTva,
        BigDecimal prixUnitaireTtc,
        String photo,
        int stockActuel,
        Integer seuilMin,
        CategorieSummaryDTO categorie
) {
    // Sous-DTO minimal : quand on liste des articles, le frontend a besoin
    // de savoir "quelle catégorie" (id + désignation pour l'affichage),
    // pas de tout l'objet Categorie avec sa propre liste d'articles
    // (qui recréerait une boucle infinie de sérialisation JSON au passage).
    public record CategorieSummaryDTO(Long id, String designation) {}
}
