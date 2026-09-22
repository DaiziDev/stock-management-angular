package com.sgs.backend.categorie;

import com.sgs.backend.article.Article;
import com.sgs.backend.common.AbstractEntity;
import com.sgs.backend.entreprise.Entreprise;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "categorie")
public class Categorie extends AbstractEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "designation", nullable = false)
    private String designation;

    @OneToMany(mappedBy = "categorie")
    private List<Article> articles;

    // Nullable pour l'instant, même raison que sur Article.
    @ManyToOne
    @JoinColumn(name = "identreprise")
    private Entreprise entreprise;
}
