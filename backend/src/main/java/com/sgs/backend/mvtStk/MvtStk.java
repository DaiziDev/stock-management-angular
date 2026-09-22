package com.sgs.backend.mvtStk;

import com.sgs.backend.article.Article;
import com.sgs.backend.common.AbstractEntity;
import com.sgs.backend.entreprise.Entreprise;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mvtstk")
public class MvtStk extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypeMouvement type;

    // Toujours positive pour ENTREE/SORTIE. Peut être négative pour un
    // AJUSTEMENT (correction d'inventaire à la baisse).
    @Column(name = "quantite", nullable = false)
    private int quantite;

    @Column(name = "datemouvement", nullable = false)
    private Instant dateMouvement;

    // Obligatoire uniquement pour AJUSTEMENT (vérifié dans le Service, pas
    // ici : une contrainte NOT NULL en base interdirait ENTREE/SORTIE, qui
    // n'ont pas de motif à saisir puisqu'ils sont générés automatiquement).
    @Column(name = "motif")
    private String motif;

    // Snapshot du stock juste après ce mouvement, figé au moment de l'écriture.
    // Indispensable pour un historique fiable : Article.stockActuel ne
    // reflète que l'état COURANT, donc sans cette copie chaque ligne de
    // l'historique afficherait le stock d'aujourd'hui au lieu du stock
    // réel à l'époque du mouvement.
    @Column(name = "stockapresmouvement", nullable = false)
    private int stockApresMouvement;

    // Référence texte libre vers la commande/vente à l'origine du mouvement
    // (ex: "CC-000004"). Pas de FK vers 3 entités différentes (CommandeClient,
    // CommandeFournisseur, Vente) qui compliquerait le modèle pour un simple
    // besoin de traçabilité en lecture.
    @Column(name = "origine")
    private String origine;

    @ManyToOne
    @JoinColumn(name = "idarticle", nullable = false)
    private Article article;

    @ManyToOne
    @JoinColumn(name = "identreprise", nullable = false)
    private Entreprise entreprise;
}
