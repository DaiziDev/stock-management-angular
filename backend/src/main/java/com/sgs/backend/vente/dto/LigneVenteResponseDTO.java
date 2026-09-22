package com.sgs.backend.vente.dto;

import java.math.BigDecimal;

public record LigneVenteResponseDTO(
        Long id,
        Long articleId,
        String articleDesignation,
        int quantite,
        BigDecimal prixUnitaire,
        BigDecimal sousTotal
) {}
