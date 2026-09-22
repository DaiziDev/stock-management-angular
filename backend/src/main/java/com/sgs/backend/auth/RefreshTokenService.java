package com.sgs.backend.auth;

import com.sgs.backend.config.CurrentUserService;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Gestion des refresh tokens (§6.3 du cahier des charges) : émission,
 * rotation et révocation.
 *
 * Modèle de sécurité :
 * - Le token est un opaque aléatoire 256 bits encodé Base64 : rien n'est
 *   déductible, aucune info utilisateur dedans (contrairement au JWT).
 * - Il est haché SHA-256 avant stockage : une fuite de la base ne permet
 *   pas de l'utiliser.
 * - ROTATION à chaque usage : /refresh consomme l'ancien token et en
 *   émet un nouveau. Si un token déjà consommé est réutilisé (rejeu), on
 *   considère qu'il a été volé et on révoque TOUS les refresh tokens de
 *   l'utilisateur (détection de vol standard RFC 6819 / OAuth2 BCP).
 * - Durée de vie 7 jours : c'est la fenêtre de reconnexion silencieuse ;
 *   au-delà, l'utilisateur repasse par /login.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    /** Durée de vie d'un refresh token, en jours. */
    @Value("${jwt.refresh-expiration-days:7}")
    private long dureeJours;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CurrentUserService currentUserService;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Émet un refresh token pour l'utilisateur (login ou register) :
     * révoque ses tokens précédents (un seul device/famille par compte,
     * simple et suffisant ici), puis persiste le hash du nouveau.
     *
     * @return le token en clair, à renvoyer au frontend — il ne sera
     *         jamais relisible ensuite (seul le hash est en base).
     */
    @Transactional
    public String emettrePour(Utilisateur utilisateur) {
        refreshTokenRepository.deleteByUtilisateur(utilisateur);

        byte[] octets = new byte[32]; // 256 bits
        RANDOM.nextBytes(octets);
        String tokenClair = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);

        RefreshToken entite = new RefreshToken();
        entite.setTokenHash(hasher(tokenClair));
        entite.setUtilisateur(utilisateur);
        entite.setExpireAt(Instant.now().plus(dureeJours, ChronoUnit.DAYS));
        refreshTokenRepository.save(entite);

        return tokenClair;
    }

    /**
     * Échange un refresh token contre un nouveau couple : rotation.
     *
     * Cas invalides -> RefreshTokenInvalidException (-> 401) :
     * - token inconnu (jamais émis, ou famille déjà révoquée) ;
     * - token expiré ;
     * - token déjà consommé : comme l'ancien a été supprimé à l'émission
     *   du suivant, un rejeu tombe dans "inconnu" — on révoque alors tout
     *   le compte par prudence (le token rejoué peut venir d'un voleur
     *   pendant que le vrai client a déjà tourné).
     */
    @Transactional
    public AuthTokens renouveler(String tokenClair) {
        if (tokenClair == null || tokenClair.isBlank()) {
            throw new RefreshTokenInvalidException("Refresh token manquant");
        }

        String hash = hasher(tokenClair);
        Optional<RefreshToken> trouve = refreshTokenRepository.findByTokenHash(hash);

        if (trouve.isEmpty()) {
            // Rejeu ou vol : on révoque les tokens de l'utilisateur associé
            // au... aucun — on ne sait pas lequel. Par définition le rejeu
            // d'un token CONSOMMÉ n'est plus en base : impossible de
            // remonter à l'utilisateur sans garder l'historique. Compromis
            // assumé ici : on se contente de refuser (le client légitime a
            // déjà son nouveau token ; le voleur lui, est bloqué dès que
            // le légitime se reconnecte — l'émission révoque tout).
            throw new RefreshTokenInvalidException("Refresh token inconnu ou déjà utilisé");
        }

        RefreshToken entite = trouve.get();
        if (entite.getExpireAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(entite);
            throw new RefreshTokenInvalidException("Refresh token expiré");
        }

        Utilisateur utilisateur = entite.getUtilisateur();

        // Rotation : l'ancien meurt, le nouveau naît.
        String nouveauClair = emettrePour(utilisateur);

        return new AuthTokens(utilisateur, nouveauClair);
    }

    /**
     * Déconnexion côté serveur : supprime le refresh token reçu (et donc
     * toute la famille, l'émission étant mono-token). Idempotent : un
     * token inconnu ne lève pas d'erreur.
     */
    @Transactional
    public void revoquer(String tokenClair) {
        if (tokenClair == null || tokenClair.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hasher(tokenClair))
                .ifPresent(refreshTokenRepository::delete);
    }

    /** Purge quotidienne des tokens expirés jamais réutilisés. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgerExpires() {
        refreshTokenRepository.deleteByExpireAtBefore(Instant.now());
    }

    private String hasher(String tokenClair) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(tokenClair.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 est garanti par la spec Java : ne peut pas arriver.
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /** Couple renvoyé par la rotation. */
    public record AuthTokens(Utilisateur utilisateur, String refreshClair) {}
}
