package com.sgs.backend.auth;

import com.sgs.backend.config.dto.LoginResponse;

/**
 * Réponse commune de POST /api/auth/login, /register et /refresh :
 * le JWT d'accès + le refresh token + le profil utilisateur.
 */
public record AuthTokensDTO(
        String token,
        String refreshToken,
        LoginResponse.UserInfo user
) {}
