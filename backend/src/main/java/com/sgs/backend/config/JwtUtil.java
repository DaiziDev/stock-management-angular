package com.sgs.backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utilitaire JWT — single source of truth pour tout ce qui touche aux tokens.
 *
 * Un token JWT contient 3 parties (séparées par des points) :
 *   HEADER  → algorithme de signature (HS256 ici)
 *   PAYLOAD → les données (email, rôle, entreprise, date d'expiration)
 *   SIGNATURE → hash du header + payload avec la secret key
 *
 * Le frontend stocke le token et l'envoie dans chaque requête :
 *   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration; // en millisecondes

    // ───────────── Extraction ─────────────

    /** Extrait n'importe quel claim du token via une Function. */
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    /** Extrait l'email (subject) du token. */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Extrait l'ID de l'entreprise depuis les claims custom. */
    public Long extractEntrepriseId(String token) {
        return extractClaim(token, claims -> claims.get("entrepriseId", Long.class));
    }

    /** Extrait le rôle depuis les claims custom. */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /** Extrait la date d'expiration du token. */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ───────────── Validation ─────────────

    /** Vérifie si le token est expiré. */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Valide un token en vérifiant :
     * 1. La signature est correcte (pas de falsification)
     * 2. Le token n'est pas expiré
     * 3. L'email correspond à l'utilisateur connecté
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Token invalide (mauvaise signature, expiré, malformé…)
            return false;
        }
    }

    // ───────────── Génération ─────────────

    /**
     * Génère un token JWT pour un utilisateur.
     *
     * Les claims personnalisés (role, entrepriseId) sont ajoutés dans le payload
     * pour que le frontend puisse les lire sans appeler le backend à chaque fois.
     */
    public String generateToken(String email, String role, Long entrepriseId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("entrepriseId", entrepriseId);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Version simplifiée pour les tests ou les cas où on a déjà un UserDetails.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername(), null, null);
    }

    // ───────────── Privé ─────────────

    /**
     * La clé de signature est dérivée du secret configuré dans application.yaml.
     * HMAC-SHA256 nécessite une clé d'au moins 256 bits (32 octets).
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
