package com.sgs.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Projections SQL utilisées en interne par DashboardService pour construire
 * GraphiquesResponseDTO. Interfaces Spring Data (pas des records JPA) :
 * chaque interface est peuplée par alias de colonne depuis les requêtes
 * @Query natives des repositories.
 */
public final class GraphiquesProjections {

    private GraphiquesProjections() {
        // Classe utilitaire : pas d'instanciation.
    }

    /** Ligne agrégée d'une journée : entrées et sorties cumulées. */
    public interface JourMouvement {
        LocalDate getDate();
        Long getEntrees();
        Long getSorties();
    }

    /** Ligne du classement des articles les plus vendus. */
    public interface TopArticleLigne {
        Long getArticleId();
        String getDesignation();
        String getCodeArticle();
        Long getQuantiteVendue();
        BigDecimal getChiffreAffaires();
    }

    /**
     * Résultat unique d'un agrégat : nb de ventes + chiffre d'affaires
     * (DashboardService.kpis, §5.1 -- calculé en SQL plutôt qu'en chargeant
     * toutes les ventes du mois en mémoire).
     */
    public interface StatsVentesLigne {
        Long getNbVentes();
        BigDecimal getChiffreAffaires();
    }
}
