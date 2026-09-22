package com.sgs.backend.utilisateur;

import com.sgs.backend.common.AbstractEntity;
import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.roles.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "utilisateur")
public class Utilisateur extends AbstractEntity {

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom", nullable = false)
    private String prenom;

    @Column(name = "login", nullable = false, unique = true)
    private String login;

    @Column(name = "motdepasse", nullable = false)
    private String motDePasse;

    @Column(name = "mail")
    private String mail;

    @Column(name = "numtel")
    private String numTel;

    @Column(name = "photo")
    private String photo;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @ManyToOne
    @JoinColumn(name = "identreprise")
    private Entreprise entreprise;
}
