package com.sgs.backend.ligneVente;

import com.sgs.backend.dashboard.dto.GraphiquesProjections.StatsVentesLigne;
import com.sgs.backend.dashboard.dto.GraphiquesProjections.TopArticleLigne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface LigneVenteRepository extends JpaRepository<LigneVente, Long> {

    /**
     * Top articles vendus sur une période, pour l'entreprise courante
     * (roadmap #13 -- "Top 5 articles les plus vendus").
     *
     * Le filtrage par entreprise passe par vente.entreprise : la ligne de
     * vente hérite du tenant de sa vente. Le classement est fait en SQL
     * (ORDER BY ... LIMIT) plutôt qu'en mémoire pour ne charger que les
     * 5 lignes voulues, quelle que soit la taille du catalogue.
     *
     * Colonnes alignées sur GraphiquesProjections.TopArticleLigne
     * (alias = getter, snake_case traduit automatiquement par Spring Data).
     */
    @Query(value = """
            SELECT lv.idarticle                                          AS article_id,
                   a.designation                                         AS designation,
                   a.codearticle                                         AS code_article,
                   SUM(lv.quantite)                                      AS quantite_vendue,
                   SUM(lv.prixunitaire * lv.quantite)                    AS chiffre_affaires
            FROM lignevente lv
            JOIN vente v  ON v.id = lv.idvente
            JOIN article a ON a.id = lv.idarticle
            WHERE v.identreprise = :entrepriseId
              AND v.datevente >= :depuis
            GROUP BY lv.idarticle, a.designation, a.codearticle
            ORDER BY quantite_vendue DESC
            LIMIT :limite
            """, nativeQuery = true)
    List<TopArticleLigne> topArticlesVendus(
            @Param("entrepriseId") Long entrepriseId,
            @Param("depuis") Instant depuis,
            @Param("limite") int limite
    );

    /**
     * KPIs mensuels du dashboard (§3.9) : nombre de ventes et chiffre
     * d'affaires du mois, agrégés en SQL (§5.1) au lieu de charger toutes
     * les ventes puis les filtrer/sommeler en Java.
     *
     * Le total passe par les LIGNES (prixunitaire x quantite), exactement
     * comme le fait VenteService.toResponseDTO pour le total d'une vente :
     * cohérence garantie entre l'affichage d'une vente et le CA agrégé.
     * SUM() sur aucune ligne renvoie NULL -> COALESCE vers 0.
     *
     * Colonnes alignées sur GraphiquesProjections.StatsVentesLigne.
     */
    @Query(value = """
            SELECT COUNT(DISTINCT v.id)                        AS nb_ventes,
                   COALESCE(SUM(lv.prixunitaire * lv.quantite), 0) AS chiffre_affaires
            FROM vente v
            JOIN lignevente lv ON lv.idvente = v.id
            WHERE v.identreprise = :entrepriseId
              AND v.datevente >= :depuis
            """, nativeQuery = true)
    StatsVentesLigne statsVentesDepuis(
            @Param("entrepriseId") Long entrepriseId,
            @Param("depuis") Instant depuis
    );
}
