package com.sgs.backend.commandeFournisseur.dto;

import java.math.BigDecimal;

public record LigneCommandeFournisseurResponseDTO(
        Long id,
        Long articleId,
        String articleDesignation,
        int quantite,
        /** Quantité déjà réceptionnée (réception partielle, §3.5). Toujours <= quantite. */
        int quantiteRecue,
        BigDecimal prixUnitaire,
        BigDecimal sousTotal
) {}
