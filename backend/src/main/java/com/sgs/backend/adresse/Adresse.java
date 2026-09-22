package com.sgs.backend.adresse;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// @Embeddable : ce n'est PAS une entité à part entière, elle n'a pas de table
// ni d'id propre. Elle est "embarquée" dans les colonnes de la table qui la contient
// (ex: Client, Fournisseur). C'est pour ça qu'elle n'étend PAS AbstractEntity.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Adresse {

    @Column(name = "adresse1")
    private String adresse1;

    @Column(name = "adresse2")
    private String adresse2;

    @Column(name = "ville")
    private String ville;

    @Column(name = "codepostal")
    private String codePostal;

    @Column(name = "pays")
    private String pays;
}
