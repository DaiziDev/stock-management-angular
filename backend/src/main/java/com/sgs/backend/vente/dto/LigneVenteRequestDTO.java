package com.sgs.backend.vente.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LigneVenteRequestDTO(
        @NotNull(message = "l'article est obligatoire")
        Long articleId,

        @NotNull(message = "la quantité est obligatoire")
        @Positive(message = "la quantité doit être supérieure à zéro")
        Integer quantite
) {}
