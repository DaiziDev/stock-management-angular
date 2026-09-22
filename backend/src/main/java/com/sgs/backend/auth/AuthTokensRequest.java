package com.sgs.backend.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps des requêtes POST /api/auth/refresh et /api/auth/logout :
 * le refresh token opaque reçu au login.
 */
public record AuthTokensRequest(
        @NotBlank(message = "Le refresh token est obligatoire")
        String refreshToken
) {}
