package com.sgs.backend.entreprise;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {

    boolean existsByNom(String nom);
}
