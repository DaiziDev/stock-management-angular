package com.sgs.backend.commandeFournisseur;

import com.sgs.backend.commande.StatutCommandeFournisseur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommandeFournisseurRepository extends JpaRepository<CommandeFournisseur, Long> {

    List<CommandeFournisseur> findByEntrepriseIdOrderByDateCommandeDesc(Long entrepriseId);

    long countByEntrepriseIdAndStatut(Long entrepriseId, StatutCommandeFournisseur statut);
}
