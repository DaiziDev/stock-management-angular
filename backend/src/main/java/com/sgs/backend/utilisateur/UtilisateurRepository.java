package com.sgs.backend.utilisateur;

import com.sgs.backend.roles.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    boolean existsByLogin(String login);

    Optional<Utilisateur> findByLogin(String login);

    List<Utilisateur> findByEntrepriseId(Long entrepriseId);

    /** Comptes rattachés à une entreprise -- console plateforme. */
    long countByEntrepriseId(Long entrepriseId);

    /** Comptes d'un rôle donné (stats plateforme : nb d'admins d'entreprises). */
    long countByRole(UserRole role);
}
