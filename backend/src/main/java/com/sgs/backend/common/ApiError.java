package com.sgs.backend.common;

import java.time.Instant;

// Format d'erreur unique renvoyé par toute l'API, quel que soit l'endroit
// où ça casse. Le frontend n'a qu'un seul format à parser pour afficher
// un message d'erreur, au lieu de devoir gérer 10 formats différents.
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {}
