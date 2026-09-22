package com.sgs.backend.dashboard;

import com.sgs.backend.commande.StatutCommandeClient;
import com.sgs.backend.commande.StatutCommandeFournisseur;
import com.sgs.backend.commandeClient.CommandeClientRepository;
import com.sgs.backend.commandeFournisseur.CommandeFournisseurRepository;
import com.sgs.backend.config.CurrentUserService;
import com.sgs.backend.dashboard.dto.DashboardKpisDTO;
import com.sgs.backend.dashboard.dto.GraphiquesProjections.JourMouvement;
import com.sgs.backend.dashboard.dto.GraphiquesProjections.StatsVentesLigne;
import com.sgs.backend.dashboard.dto.GraphiquesProjections.TopArticleLigne;
import com.sgs.backend.dashboard.dto.GraphiquesResponseDTO;
import com.sgs.backend.dashboard.dto.GraphiquesResponseDTO.EntreeSortieJourDTO;
import com.sgs.backend.dashboard.dto.GraphiquesResponseDTO.TopArticleDTO;
import com.sgs.backend.ligneVente.LigneVenteRepository;
import com.sgs.backend.mvtStk.MvtStkRepository;
import com.sgs.backend.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrège des données déjà tenant-scopées par les Services sous-jacents --
 * ce Service ne fait aucun filtrage lui-même, il combine des résultats déjà
 * sûrs (chacun applique son propre CurrentUserService en interne).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** Fenêtre du graphique entrées/sorties (cahier des charges §3.9 : période sélectionnable). */
    private static final int NB_JOURS_GRAPHIQUES = 30;

    /** Taille du classement "top articles vendus" (roadmap #13 : top 5). */
    private static final int TOP_ARTICLES_LIMITE = 5;

    private final StockService stockService;
    private final CommandeClientRepository commandeClientRepository;
    private final CommandeFournisseurRepository commandeFournisseurRepository;
    private final CurrentUserService currentUserService;
    private final MvtStkRepository mvtStkRepository;
    private final LigneVenteRepository ligneVenteRepository;

    public DashboardKpisDTO kpis() {
        Long entrepriseId = currentUserService.getEntrepriseId();

        BigDecimal valeurStock = stockService.valorisation().valeurTotale();
        int nbArticlesEnAlerte = stockService.alertes().size();
        long nbCommandesClientEnCours = commandeClientRepository.countByEntrepriseIdAndStatut(entrepriseId, StatutCommandeClient.EN_COURS);
        long nbCommandesFournisseurEnAttente = commandeFournisseurRepository.countByEntrepriseIdAndStatut(entrepriseId, StatutCommandeFournisseur.EN_ATTENTE);

        // CA + nb de ventes du mois : agrégés en SQL (§5.1) -- on ne charge
        // plus toutes les ventes en mémoire pour les filtrer/sommeler ici.
        Instant debutDuMois = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        StatsVentesLigne stats = ligneVenteRepository.statsVentesDepuis(entrepriseId, debutDuMois);

        return new DashboardKpisDTO(
                valeurStock,
                nbArticlesEnAlerte,
                nbCommandesClientEnCours,
                nbCommandesFournisseurEnAttente,
                stats.getNbVentes() != null ? stats.getNbVentes() : 0L,
                stats.getChiffreAffaires() != null ? stats.getChiffreAffaires() : BigDecimal.ZERO
        );
    }

    /**
     * Données des graphiques du cahier des charges (§3.9) : évolution des
     * entrées / sorties de stock et top articles vendus, sur 30 jours.
     * Tout est agrégé en SQL, tenant-scopé via l'entreprise du JWT.
     */
    public GraphiquesResponseDTO graphiques() {
        Long entrepriseId = currentUserService.getEntrepriseId();
        ZoneId fuseau = ZoneId.systemDefault();

        Instant depuis = LocalDate.now(fuseau).minusDays(NB_JOURS_GRAPHIQUES - 1L)
                .atStartOfDay(fuseau).toInstant();

        // 1. Série entrées / sorties : la base ne renvoie que les journées
        // qui ont des mouvements -- on comble les journées vides à 0 pour
        // que le graphique affiche 30 points réguliers.
        Map<LocalDate, JourMouvement> parJour = new HashMap<>();
        for (JourMouvement ligne : mvtStkRepository.evolutionQuotidienneEntreesSorties(
                entrepriseId, depuis, fuseau.getId())) {
            parJour.put(ligne.getDate(), ligne);
        }

        List<EntreeSortieJourDTO> evolution = new ArrayList<>(NB_JOURS_GRAPHIQUES);
        for (int i = NB_JOURS_GRAPHIQUES - 1; i >= 0; i--) {
            LocalDate jour = LocalDate.now(fuseau).minusDays(i);
            JourMouvement ligne = parJour.get(jour);
            evolution.add(new EntreeSortieJourDTO(
                    jour,
                    ligne != null ? ligne.getEntrees() : 0L,
                    ligne != null ? ligne.getSorties() : 0L
            ));
        }

        // 2. Top articles vendus sur la même période.
        List<TopArticleDTO> topArticles = ligneVenteRepository.topArticlesVendus(
                        entrepriseId, depuis, TOP_ARTICLES_LIMITE).stream()
                .map(l -> new TopArticleDTO(
                        l.getArticleId(),
                        l.getDesignation(),
                        l.getCodeArticle(),
                        l.getQuantiteVendue(),
                        l.getChiffreAffaires()
                ))
                .toList();

        return new GraphiquesResponseDTO(evolution, topArticles);
    }
}
