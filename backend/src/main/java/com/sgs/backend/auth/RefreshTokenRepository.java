package com.sgs.backend.auth;

import com.sgs.backend.utilisateur.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Rotation : on supprime l'ancien token au moment d'émettre le nouveau
     * (un seul token actif par utilisateur). Doit être appelé dans une
     * transaction (c'est le cas : RefreshTokenService est @Transactional).
     */
    void deleteByUtilisateur(Utilisateur utilisateur);

    /**
     * Purge quotidienne des tokens expirés (voir RefreshTokenService) —
     * DELETE dérivé : rien n'est chargé en mémoire.
     */
    long deleteByExpireAtBefore(Instant instant);
}
