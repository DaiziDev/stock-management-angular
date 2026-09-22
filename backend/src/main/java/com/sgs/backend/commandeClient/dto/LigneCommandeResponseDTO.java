package com.sgs.backend.commandeClient.dto;

import java.math.BigDecimal;

public record LigneCommandeResponseDTO(
        Long id,
        Long articleId,
        String articleDesignation,
        int quantite,
        BigDecimal prixUnitaire,
        BigDecimal sousTotal
) {}
