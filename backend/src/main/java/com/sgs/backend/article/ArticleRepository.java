package com.sgs.backend.article;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByCodeArticle(String codeArticle);

    List<Article> findByEntrepriseId(Long entrepriseId);

    /** Comptage global, toutes entreprises confondues -- stats plateforme. */
    long countByEntrepriseIsNotNull();

    /**
     * Articles en alerte (stockActuel <= seuilMin), toutes entreprises.
     * Pas dérivable par Spring Data (comparaison entre 2 colonnes) : on
     * charge et on filtre en mémoire -- suffisant à l'échelle d'un parc.
     */
    default long countAlertes() {
        return findByEntrepriseIsNotNull().stream()
                .filter(a -> a.getSeuilMin() != null && a.getStockActuel() <= a.getSeuilMin())
                .count();
    }

    /** Liste des articles rattachés à une entreprise (pour compter les alertes). */
    List<Article> findByEntrepriseIsNotNull();
}
