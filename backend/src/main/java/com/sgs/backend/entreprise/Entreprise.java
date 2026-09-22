package com.sgs.backend.entreprise;

import com.sgs.backend.adresse.Adresse;
import com.sgs.backend.common.AbstractEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "entreprise")
public class Entreprise extends AbstractEntity {

    @Column(name = "nom", nullable = false, unique = true)
    private String nom;

    @Embedded
    private Adresse adresse;

    @Column(name = "mail")
    private String mail;

    @Column(name = "numtel")
    private String numTel;

    // Pas de relation @OneToMany ici vers Utilisateur/Article/Client/etc.
    // Volontairement : on ne va PAS naviguer "depuis" Entreprise vers toutes
    // ses données (ça deviendrait une classe god-object). Le filtrage
    // multi-tenant se fera dans l'autre sens : chaque entite (Client,
    // Article...) porte un champ `entreprise`, et on interroge SES
    // repositories a elle avec un filtre sur cet id -- pas l'inverse.
}
