package com.sgs.backend.fournisseur;

import com.sgs.backend.adresse.Adresse;
import com.sgs.backend.common.AbstractEntity;
import com.sgs.backend.entreprise.Entreprise;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "fournisseur")
public class Fournisseur extends AbstractEntity {

    @Column(name = "nom", nullable = false)
    private String nom;

    @Embedded
    private Adresse adresse;

    @Column(name = "mail")
    private String mail;

    @Column(name = "numtel")
    private String numTel;

    // Pas de @OneToMany vers CommandeFournisseur pour l'instant : cette
    // entité est encore une coquille vide (roadmap #8). Même raison que le
    // TODO sur CommandeClient -- Hibernate refuserait de démarrer avec un
    // mappedBy pointant vers un champ qui n'existe pas encore.
    @ManyToOne
    @JoinColumn(name = "identreprise")
    private Entreprise entreprise;
}
