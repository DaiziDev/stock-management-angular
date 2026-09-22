package com.sgs.backend.commandeClient;

import com.sgs.backend.client.Client;
import com.sgs.backend.commande.StatutCommandeClient;
import com.sgs.backend.common.AbstractEntity;
import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.ligneCommandeClient.LigneCommandeClient;
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
@Table(name = "commandeclient")
public class CommandeClient extends AbstractEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "datecommande", nullable = false)
    private Instant dateCommande;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutCommandeClient statut;

    @ManyToOne
    @JoinColumn(name = "idclient")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "identreprise")
    private Entreprise entreprise;

    // cascade=ALL + orphanRemoval : les lignes n'ont pas de cycle de vie
    // propre en dehors de leur commande -- les créer/supprimer avec elle
    // évite d'avoir à gérer LigneCommandeClientRepository séparément
    // depuis CommandeClientService.
    @OneToMany(mappedBy = "commandeClient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommandeClient> lignes;
}
