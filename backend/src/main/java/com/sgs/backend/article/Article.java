package com.sgs.backend.article;

import com.sgs.backend.categorie.Categorie;
import com.sgs.backend.common.AbstractEntity;
import com.sgs.backend.entreprise.Entreprise;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "article")
public class Article extends AbstractEntity {

    @Column(name = "codearticle", nullable = false, unique = true)
    private String codeArticle;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "prixunitaireht", nullable = false)
    private BigDecimal prixUnitaireHt;

    @Column(name = "tauxtva", nullable = false)
    private BigDecimal tauxTva;

    @Column(name = "prixunitairettc", nullable = false)
    private BigDecimal prixUnitaireTtc;

    // Stockée en base sous forme de data URL (base64) : la colonne doit
    // être TEXT, pas varchar(255). LONGVARCHAR force Hibernate à créer
    // une colonne TEXT/character large sur une base neuve ; pour une base
    // existante, DataInitializer élargit la colonne au démarrage.
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "photo", columnDefinition = "text")
    private String photo;

    @ManyToOne
    @JoinColumn(name = "idcategorie")
    private Categorie categorie;

    // Jamais modifié directement (pas de setter appelé depuis ArticleService) :
    // seul MvtStkService fait bouger cette valeur, et toujours avec une
    // trace (un MvtStk) à l'appui. int (pas Integer) pour que 0 soit la
    // valeur par défaut sans code supplémentaire à la création.
    @Column(name = "stockactuel", nullable = false)
    private int stockActuel;

    // Nullable : un seuil non configuré signifie "pas d'alerte pour cet
    // article", pas "seuil = 0".
    @Column(name = "seuilmin")
    private Integer seuilMin;

    // Nullable pour l'instant : les lignes créées avant l'introduction du
    // filtrage multi-tenant n'ont pas d'entreprise. Un article sans
    // entreprise n'apparaît simplement dans aucune liste filtrée.
    @ManyToOne
    @JoinColumn(name = "identreprise")
    private Entreprise entreprise;
}
