package com.sgs.backend.vente;

import com.sgs.backend.client.Client;
import com.sgs.backend.common.AbstractEntity;
import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.ligneVente.LigneVente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "vente")
public class Vente extends AbstractEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "datevente", nullable = false)
    private Instant dateVente;

    // Nullable : une vente au comptoir n'a pas forcément de client identifié.
    @ManyToOne
    @JoinColumn(name = "idclient")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "identreprise")
    private Entreprise entreprise;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneVente> lignes;
}
