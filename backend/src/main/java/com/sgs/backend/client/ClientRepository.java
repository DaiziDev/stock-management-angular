package com.sgs.backend.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByEntrepriseId(Long entrepriseId);

    /** Comptage global, toutes entreprises confondues -- stats plateforme. */
    long countByEntrepriseIsNotNull();
}
