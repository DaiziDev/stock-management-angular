package com.sgs.backend.ligneVente;

import com.sgs.backend.article.Article;
import com.sgs.backend.common.AbstractEntity;
import com.sgs.backend.vente.Vente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "lignevente")
public class LigneVente extends AbstractEntity {

    @Column(name = "quantite", nullable = false)
    private int quantite;

    // Copié depuis Article.prixUnitaireTtc au moment de la vente.
    @Column(name = "prixunitaire", nullable = false)
    private BigDecimal prixUnitaire;

    @ManyToOne
    @JoinColumn(name = "idarticle", nullable = false)
    private Article article;

    @ManyToOne
    @JoinColumn(name = "idvente")
    private Vente vente;
}
