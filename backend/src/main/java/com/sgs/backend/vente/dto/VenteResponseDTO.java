package com.sgs.backend.vente.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record VenteResponseDTO(
        Long id,
        String code,
        Instant dateVente,
        Long clientId,
        String clientNom,
        List<LigneVenteResponseDTO> lignes,
        BigDecimal total
) {}
