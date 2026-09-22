package com.sgs.backend.commandeFournisseur.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CommandeFournisseurRequestDTO(
        @NotNull(message = "le fournisseur est obligatoire")
        Long fournisseurId,

        @NotEmpty(message = "une commande doit contenir au moins une ligne")
        @Valid
        List<LigneCommandeFournisseurRequestDTO> lignes
) {}
