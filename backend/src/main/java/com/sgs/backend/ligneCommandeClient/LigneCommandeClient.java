package com.sgs.backend.ligneCommandeClient;

import com.sgs.backend.article.Article;
import com.sgs.backend.commandeClient.CommandeClient;
import com.sgs.backend.common.AbstractEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "lignecommandeclient")
public class LigneCommandeClient extends AbstractEntity {

    @Column(name = "quantite", nullable = false)
    private int quantite;

    // Copié depuis Article.prixUnitaireTtc au moment de la commande, jamais
    // recalculé après coup : si le prix de l'article change ensuite, cette
    // commande garde le prix auquel elle a été passée (roadmap #7).
    @Column(name = "prixunitaire", nullable = false)
    private BigDecimal prixUnitaire;

    @ManyToOne
    @JoinColumn(name = "idarticle", nullable = false)
    private Article article;

    @ManyToOne
    @JoinColumn(name = "idcommandeclient")
    private CommandeClient commandeClient;
}
