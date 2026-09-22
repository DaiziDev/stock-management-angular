package com.sgs.backend.vente.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record VenteRequestDTO(
        // Nullable : vente au comptoir sans client identifié.
        Long clientId,

        @NotEmpty(message = "une vente doit contenir au moins une ligne")
        @Valid
        List<LigneVenteRequestDTO> lignes
) {}
