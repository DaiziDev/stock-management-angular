package com.sgs.backend.fournisseur;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    List<Fournisseur> findByEntrepriseId(Long entrepriseId);
}
