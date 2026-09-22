package com.sgs.backend.ligneCommandeFournisseur;

import com.sgs.backend.article.Article;
import com.sgs.backend.commandeFournisseur.CommandeFournisseur;
import com.sgs.backend.common.AbstractEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "lignecommandefournisseur")
public class LigneCommandeFournisseur extends AbstractEntity {

    @Column(name = "quantite", nullable = false)
    private int quantite;

    /**
     * Quantité déjà réceptionnée pour cette ligne (§3.5 : réception partielle).
     * Toujours <= quantite. Une ligne est "satisfaite" quand les deux sont égales.
     */
    @Column(name = "quantiterecue", nullable = false)
    private int quantiteRecue = 0;

    // Copié depuis Article.prixUnitaireHt (prix d'achat, pas le prix de
    // vente TTC utilisé côté CommandeClient/Vente) au moment de la commande.
    @Column(name = "prixunitaire", nullable = false)
    private BigDecimal prixUnitaire;

    @ManyToOne
    @JoinColumn(name = "idarticle", nullable = false)
    private Article article;

    @ManyToOne
    @JoinColumn(name = "idcommandefournisseur")
    private CommandeFournisseur commandeFournisseur;
}
