package com.sgs.backend.vente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

import java.util.List;

public interface VenteRepository extends JpaRepository<Vente, Long> {

    List<Vente> findByEntrepriseIdOrderByDateVenteDesc(Long entrepriseId);

    /** Comptage global, toutes entreprises confondues -- stats plateforme. */
    long countByEntrepriseIsNotNull();

    /** Ventes du mois courant, toutes entreprises -- stats plateforme. */
    List<Vente> findByEntrepriseIsNotNullAndDateVenteGreaterThanEqual(Instant depuis);
}
