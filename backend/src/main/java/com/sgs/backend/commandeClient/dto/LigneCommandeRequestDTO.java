package com.sgs.backend.commandeClient.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LigneCommandeRequestDTO(
        @NotNull(message = "l'article est obligatoire")
        Long articleId,

        @NotNull(message = "la quantité est obligatoire")
        @Positive(message = "la quantité doit être supérieure à zéro")
        Integer quantite
) {}
