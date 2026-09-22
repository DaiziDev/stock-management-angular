package com.sgs.backend.commandeFournisseur;

import com.sgs.backend.commande.StatutCommandeFournisseur;
import com.sgs.backend.common.AbstractEntity;
import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.fournisseur.Fournisseur;
import com.sgs.backend.ligneCommandeFournisseur.LigneCommandeFournisseur;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "commandefournisseur")
public class CommandeFournisseur extends AbstractEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "datecommande", nullable = false)
    private Instant dateCommande;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutCommandeFournisseur statut;

    @ManyToOne
    @JoinColumn(name = "idfournisseur")
    private Fournisseur fournisseur;

    @ManyToOne
    @JoinColumn(name = "identreprise")
    private Entreprise entreprise;

    @OneToMany(mappedBy = "commandeFournisseur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommandeFournisseur> lignes;
}
