package com.sgs.backend.ligneCommandeFournisseur;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Accès aux lignes de commande fournisseur. Créé pour la réception partielle
 * (§3.5) : les quantités reçues sont validées ligne par ligne. Jusqu'ici les
 * lignes étaient uniquement accédées via CommandeFournisseur.getLignes().
 */
public interface LigneCommandeFournisseurRepository extends JpaRepository<LigneCommandeFournisseur, Long> {
}
