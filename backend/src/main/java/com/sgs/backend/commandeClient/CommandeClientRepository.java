package com.sgs.backend.commandeClient;

import com.sgs.backend.commande.StatutCommandeClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommandeClientRepository extends JpaRepository<CommandeClient, Long> {

    List<CommandeClient> findByEntrepriseIdOrderByDateCommandeDesc(Long entrepriseId);

    long countByEntrepriseIdAndStatut(Long entrepriseId, StatutCommandeClient statut);

    /** Commandes en cours, toutes entreprises confondues -- stats plateforme. */
    long countByEntrepriseIsNotNullAndStatut(StatutCommandeClient statut);
}
