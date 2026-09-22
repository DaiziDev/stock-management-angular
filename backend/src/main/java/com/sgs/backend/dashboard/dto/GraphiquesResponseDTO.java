package com.sgs.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Réponse de GET /api/dashboard/graphiques : les deux graphiques du tableau
 * de bord prévus par le cahier des charges (§3.9) :
 * - l'évolution temporelle des entrées / sorties de stock ;
 * - le top articles les plus vendus (quantité vendue + CA généré).
 */
public record GraphiquesResponseDTO(
        List<EntreeSortieJourDTO> evolutionStock,
        List<TopArticleDTO> topArticles
) {
    /** Un point de la série entrées / sorties (une journée). */
    public record EntreeSortieJourDTO(
            LocalDate date,
            long entrees,
            long sorties
    ) {}

    /** Un article du classement des meilleures ventes. */
    public record TopArticleDTO(
            Long articleId,
            String designation,
            String codeArticle,
            long quantiteVendue,
            BigDecimal chiffreAffaires
    ) {}
}
