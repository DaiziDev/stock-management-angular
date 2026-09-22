package com.sgs.backend.mvtStk;

import com.sgs.backend.dashboard.dto.GraphiquesProjections.JourMouvement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface MvtStkRepository extends JpaRepository<MvtStk, Long> {

    List<MvtStk> findByEntrepriseIdOrderByDateMouvementDesc(Long entrepriseId);

    /**
     * Variantes filtrées du findAll (§5.1) : le filtrage article / type de
     * mouvement est poussé en SQL plutôt que fait en mémoire, pour ne pas
     * charger tout l'historique de mouvements de l'entreprise à chaque appel.
     * La combinaison des filtres est gérée par le Service qui choisit la
     * requête dérivée correspondante (évite un "IS NULL" fragile en SQL).
     */
    List<MvtStk> findByEntrepriseIdAndArticleIdOrderByDateMouvementDesc(Long entrepriseId, Long articleId);

    List<MvtStk> findByEntrepriseIdAndTypeOrderByDateMouvementDesc(Long entrepriseId, TypeMouvement type);

    List<MvtStk> findByEntrepriseIdAndArticleIdAndTypeOrderByDateMouvementDesc(Long entrepriseId, Long articleId, TypeMouvement type);

    /**
     * Évolution quotidienne des entrées / sorties de stock depuis une date,
     * pour l'entreprise courante (cahier des charges §3.9 : graphique
     * temporel de l'évolution des entrées et sorties).
     *
     * Les AJUSTEMENTS ne sont pas inclus : le graphique mesure le flux
     * physique de marchandises (réceptions / ventes), pas les corrections
     * d'inventaire, qui sont une opération administrative.
     *
     * La journée est calculée côté base : Hibernate 6 stocke Instant en
     * `timestamp with time zone`, donc un seul `AT TIME ZONE :fuseau` le
     * convertit en heure locale du fuseau applicatif (passé en paramètre
     * pour que les journées suivent le fuseau de l'utilisateur, pas celui
     * du serveur SQL), puis ::date le regroupe par jour civil.
     *
     * Colonnes alignées sur GraphiquesProjections.JourMouvement.
     */
    @Query(value = """
            SELECT (m.datemouvement AT TIME ZONE :fuseau)::date AS date,
                   SUM(CASE WHEN m.type = 'ENTREE'  THEN m.quantite ELSE 0 END) AS entrees,
                   SUM(CASE WHEN m.type = 'SORTIE'  THEN m.quantite ELSE 0 END) AS sorties
            FROM mvtstk m
            WHERE m.identreprise = :entrepriseId
              AND m.datemouvement >= :depuis
              AND m.type IN ('ENTREE', 'SORTIE')
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<JourMouvement> evolutionQuotidienneEntreesSorties(
            @Param("entrepriseId") Long entrepriseId,
            @Param("depuis") Instant depuis,
            @Param("fuseau") String fuseau
    );
}
