package com.sgs.backend.auth;

/**
 * Levée quand un refresh token est absent de la base, expiré, ou déjà
 * consommé (rejeu = suspicion de vol). Attrapée par GlobalExceptionHandler
 * -> 401 : le frontend doit alors demander une reconnexion, jamais retenter
 * indéfiniment.
 */
public class RefreshTokenInvalidException extends RuntimeException {
    public RefreshTokenInvalidException(String message) {
        super(message);
    }
}
