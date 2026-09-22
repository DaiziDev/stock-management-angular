package com.sgs.backend.client;

import com.sgs.backend.adresse.Adresse;
import com.sgs.backend.commandeClient.CommandeClient;
import com.sgs.backend.common.AbstractEntity;
import com.sgs.backend.entreprise.Entreprise;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "client")
public class Client extends AbstractEntity {

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom", nullable = false)
    private String prenom;

    @Embedded
    private Adresse adresse;

    @Column(name = "photo")
    private String photo;

    @Column(name = "mail")
    private String mail;

    @Column(name = "numtel")
    private String numTel;

    @OneToMany(mappedBy = "client")
    private List<CommandeClient> commandeClients;

    @ManyToOne
    @JoinColumn(name = "identreprise")
    private Entreprise entreprise;
}
