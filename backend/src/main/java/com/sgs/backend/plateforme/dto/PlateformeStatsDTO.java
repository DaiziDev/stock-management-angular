package com.sgs.backend.plateforme.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Indicateurs globaux de la plateforme (vue SUPER_ADMIN) -- enrichis pour le
 * tableau de bord de la console.
 *
 * Ce sont des compteurs PLATEFORME : ils agrègent toutes les entreprises
 * confondues -- c'est voulu et légitime ici, contrairement aux endpoints
 * métier où tout doit rester tenant-scopé (RG-10). Le SUPER_ADMIN n'est
 * rattaché à aucune entreprise : il est précisément le seul à avoir le
 * droit de voir la vue d'ensemble.
 */
public record PlateformeStatsDTO(
        // ── Parc ──
        long nbEntreprises,
        long nbUtilisateurs,
        long nbAdmins,

        // ── Activité agrégée du parc ──
        long nbClients,
        long nbArticles,
        long nbVentes,

        /** Ventes enregistrées depuis le 1er du mois courant (toutes entreprises). */
        long nbVentesDuMois,

        /** Chiffre d'affaires TTC cumulé du mois courant, toutes entreprises. */
        BigDecimal chiffreAffairesDuMois,

        /** Articles sous / au seuil d'alerte de stock, toutes entreprises. */
        long nbArticlesEnAlerte,

        /** Commandes clients EN_COURS, toutes entreprises. */
        long nbCommandesEnCours,

        // ── Croissance / récence ──
        /** Entreprises onboardées depuis le 1er du mois courant. */
        long nbNouvellesEntreprisesDuMois,

        /** Dernières entreprises onboardées (pour le panel "récemment ajoutées"). */
        List<EntrepriseRecenteDTO> dernieresEntreprises
) {
    /**
     * Résumé compact d'une entreprise récemment onboardée : juste ce qu'il
     * faut pour une carte du dashboard (lien vers la fiche complète).
     */
    public record EntrepriseRecenteDTO(
            Long id,
            String nom,
            String mail,
            String ville,
            long nbUtilisateurs,
            LocalDateTime createdAt
    ) {}
}
