package com.sgs.backend.auth;

import com.sgs.backend.common.AbstractEntity;
import com.sgs.backend.utilisateur.Utilisateur;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Refresh token du flux JWT (§6.3 du cahier des charges) : un opaque
 * aléatoire, stocké en clair côté base mais haché SHA-256 AVANT écriture,
 * pour qu'un vol de la base ne permette pas de forger des sessions.
 *
 * Durée de vie longue (7 jours) contre 24h pour le JWT d'accès : quand le
 * JWT expire, le frontend échange ce token contre un nouveau couple
 * access/refresh (rotation -- voir RefreshTokenService).
 *
 * Pas de statut "révoqué" : une utilisation d'un token déjà consommé
 * (rejeu) ou inconnu révoque la FAMILLE entière d'un utilisateur, ce qui
 * est le comportement de détection de vol attendu.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "refreshtoken")
public class RefreshToken extends AbstractEntity {

    /** SHA-256 hex du token réel — le token en clair n'est JAMAIS stocké. */
    @Column(name = "tokenhash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "idutilisateur", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "expireat", nullable = false)
    private Instant expireAt;
}
